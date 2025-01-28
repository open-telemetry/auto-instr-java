/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations;

import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.KotlinCoroutineUtil.isKotlinSuspendMethod;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AsmApi;
import io.opentelemetry.javaagent.instrumentation.instrumentationannotations.AnnotationExcludedMethods;
import io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations.SpanAttributeUtil.Parameter;
import java.util.Arrays;
import java.util.List;
import kotlin.coroutines.Continuation;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

class WithSpanInstrumentation implements TypeInstrumentation {
  // whether to check the transformed bytecode with asm CheckClassAdapter
  private static final boolean CHECK_CLASS =
      AgentInstrumentationConfig.get()
          .getBoolean(
              "otel.instrumentation.kotlinx-coroutines.check-class",
              AgentInstrumentationConfig.get().getBoolean("otel.javaagent.debug", false));

  private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;
  // this matcher matches all methods that should be excluded from transformation
  private final ElementMatcher.Junction<MethodDescription> excludedMethodsMatcher;

  WithSpanInstrumentation() {
    annotatedMethodMatcher =
        isAnnotatedWith(named("application.io.opentelemetry.instrumentation.annotations.WithSpan"));
    excludedMethodsMatcher = AnnotationExcludedMethods.configureExcludedMethods();
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return not(nameStartsWith("kotlin.coroutines."))
        .and(
            declaresMethod(
                annotatedMethodMatcher
                    .and(isKotlinSuspendMethod())
                    .and(not(excludedMethodsMatcher))));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        none(), WithSpanInstrumentation.class.getName() + "$InitAdvice");

    transformer.applyTransformer(
        (builder, typeDescription, classLoader, javaModule, protectionDomain) ->
            builder.visit(
                new AsmVisitorWrapper() {
                  @Override
                  public int mergeWriter(int flags) {
                    return flags | ClassWriter.COMPUTE_MAXS;
                  }

                  @Override
                  @CanIgnoreReturnValue
                  public int mergeReader(int flags) {
                    return flags;
                  }

                  @Override
                  public ClassVisitor wrap(
                      TypeDescription instrumentedType,
                      ClassVisitor classVisitor,
                      Implementation.Context implementationContext,
                      TypePool typePool,
                      FieldList<FieldDescription.InDefinedShape> fields,
                      MethodList<?> methods,
                      int writerFlags,
                      int readerFlags) {
                    if (CHECK_CLASS) {
                      classVisitor = new CheckClassAdapter(classVisitor);
                    }
                    // we are using a visitor that converts compressed frames into expanded frames
                    // because WithSpanClassVisitor uses GeneratorAdapter for adding new local
                    // variables that requires expanded frames. We are not using
                    // ClassReader.EXPAND_FRAMES because ExceptionHandlers class generates
                    // compressed F_SAME frame that we can't easily replace with an expanded frame
                    // because we don't know what locals are available at that point.
                    return new ExpandFramesClassVisitor(new WithSpanClassVisitor(classVisitor));
                  }
                }));
  }

  @SuppressWarnings("unused")
  public static class InitAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter() {
      // this advice is here only to get AnnotationInstrumentationHelper injected
      AnnotationInstrumentationHelper.init();
    }
  }

  private static class WithSpanClassVisitor extends ClassVisitor {
    String className;

    WithSpanClassVisitor(ClassVisitor cv) {
      super(AsmApi.VERSION, cv);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      className = name;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
      // firstly check whether this method could be a suspend method
      // kotlin suspend methods take kotlin.coroutines.Continuation as last argument and return
      // java.lang.Object
      Type[] argumentTypes = Type.getArgumentTypes(descriptor);
      if (argumentTypes.length > 0
          && "kotlin/coroutines/Continuation"
              .equals(argumentTypes[argumentTypes.length - 1].getInternalName())
          && "java/lang/Object".equals(Type.getReturnType(descriptor).getInternalName())) {
        // store method in MethodNode, so we could test whether it has the WithSpan annotation and
        // depending on that either instrument it or leave it as it is
        return new MethodNode(api, access, name, descriptor, signature, exceptions) {
          @Override
          public void visitEnd() {
            super.visitEnd();

            MethodVisitor mv = target;
            if (hasWithSpanAnnotation(this)) {
              mv = instrument(mv, this, className);
            }
            this.accept(mv);
          }
        };
      }

      return target;
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String annotationDesc) {
      if (annotations != null) {
        for (AnnotationNode annotationNode : annotations) {
          if (annotationDesc.equals(annotationNode.desc)) {
            return true;
          }
        }
      }
      return false;
    }

    private static boolean hasWithSpanAnnotation(MethodNode methodNode) {
      return hasAnnotation(
          methodNode.visibleAnnotations,
          "Lapplication/io/opentelemetry/instrumentation/annotations/WithSpan;");
    }

    private static MethodVisitor instrument(
        MethodVisitor target, MethodNode source, String className) {
      // collect method arguments with @SpanAttribute annotation
      List<Parameter> annotatedParameters = SpanAttributeUtil.collectAnnotatedParameters(source);

      String methodName = source.name;
      MethodNode methodNode =
          new MethodNode(
              source.access,
              source.name,
              source.desc,
              source.signature,
              source.exceptions.toArray(new String[0]));
      GeneratorAdapter generatorAdapter =
          new GeneratorAdapter(
              AsmApi.VERSION, methodNode, source.access, source.name, source.desc) {
            int requestLocal;
            int ourContinuationLocal;
            int contextLocal;
            int scopeLocal;
            int lastLocal;

            final Label start = new Label();
            final Label handler = new Label();

            String withSpanValue = null;
            String spanKind = null;

            @Override
            public void visitCode() {
              super.visitCode();
              // add our local variables after method arguments, this will shift rest of the locals
              requestLocal = newLocal(Type.getType(Object.class));
              ourContinuationLocal = newLocal(Type.getType(Continuation.class));
              contextLocal = newLocal(Type.getType(Context.class));
              scopeLocal = newLocal(Type.getType(Scope.class));
              // set lastLocal to the last local we added
              lastLocal = scopeLocal;

              visitLabel(start);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
              visitLabel(handler);
              visitTryCatchBlock(start, handler, handler, null);
              super.visitMaxs(maxStack, maxLocals);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
              AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
              // remember value and kind from the @WithSpan annotation
              if ("Lapplication/io/opentelemetry/instrumentation/annotations/WithSpan;"
                  .equals(descriptor)) {
                return new AnnotationVisitor(api, annotationVisitor) {
                  @Override
                  public void visit(String name, Object value) {
                    if ("value".equals(name) && value instanceof String) {
                      withSpanValue = (String) value;
                    }
                    super.visit(name, value);
                  }

                  @Override
                  public void visitEnum(String name, String descriptor, String value) {
                    if ("kind".equals(name)
                        && "Lapplication/io/opentelemetry/api/trace/SpanKind;".equals(descriptor)) {
                      spanKind = value;
                    }
                    super.visitEnum(name, descriptor, value);
                  }
                };
              }
              return annotationVisitor;
            }

            @Override
            public void visitEnd() {
              super.visitEnd();

              // If a suspend method does not contain any blocking operations or has no code after
              // the blocking operation it gets compiled to a regular method that we instrument the
              // same way as the regular @WithSpan handling does. We create the span at the start of
              // the method and end it in before every return instruction and in exception handler.
              // If a suspend method has a blocking operation and code that needs to be executed
              // after it, we start the span only when the coroutine was started, on resume we just
              // activate the scope. We end the span when coroutine completes, otherwise we only
              // close the scope.
              // First we'll search for a bytecode sequence that looks like
              // 64: aload         6
              // 66: getfield      #444                // Field
              // io/opentelemetry/javaagent/instrumentation/kotlinxcoroutines/KotlinCoroutinesInstrumentationTest$b2$1.label:I
              // 69: tableswitch   { // 0 to 1
              //                0: 92
              //                1: 181
              //          default: 210
              // We are interested in the continuation local (here slot 6) and the value of the
              // label field. To get the value of the label we'll insert our code between the
              // getfield and tableswitch instructions.
              int continuationLocal = -1;
              AbstractInsnNode insertAfterInsn = null;
              for (int i = 1; i < methodNode.instructions.size() - 1; i++) {
                AbstractInsnNode instruction = methodNode.instructions.get(i);
                if (instruction.getOpcode() == Opcodes.GETFIELD
                    && "label".equals(((FieldInsnNode) instruction).name)
                    && "I".equals(((FieldInsnNode) instruction).desc)) {
                  if (methodNode.instructions.get(i + 1).getOpcode() != Opcodes.TABLESWITCH) {
                    continue;
                  }
                  if (methodNode.instructions.get(i - 1).getOpcode() != Opcodes.ALOAD) {
                    continue;
                  }
                  insertAfterInsn = instruction;
                  continuationLocal = ((VarInsnNode) methodNode.instructions.get(i - 1)).var;
                  break;
                }
              }

              boolean hasBlockingOperation = insertAfterInsn != null && continuationLocal != -1;

              // initialize our local variables, start span and open scope
              {
                MethodNode temp = new MethodNode();
                if (hasBlockingOperation) {
                  // value of label is on stack
                  // label is used in call to enterCoroutine and later in @SpanAttribute handling
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitVarInsn(Opcodes.ALOAD, continuationLocal);
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitVarInsn(Opcodes.ASTORE, ourContinuationLocal);
                } else {
                  // nothing on stack, we are inserting code at the start of the method
                  // we'll use 0 for label and null for continuation object
                  temp.visitInsn(Opcodes.ICONST_0);
                  temp.visitInsn(Opcodes.ICONST_0);
                  temp.visitInsn(Opcodes.ACONST_NULL);
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitVarInsn(Opcodes.ASTORE, ourContinuationLocal);
                }
                temp.visitLdcInsn(Type.getObjectType(className));
                temp.visitLdcInsn(methodName);
                if (withSpanValue != null) {
                  temp.visitLdcInsn(withSpanValue);
                } else {
                  temp.visitInsn(Opcodes.ACONST_NULL);
                }
                if (spanKind != null) {
                  temp.visitLdcInsn(spanKind);
                } else {
                  temp.visitInsn(Opcodes.ACONST_NULL);
                }
                visitInvokeHelperMethod(
                    temp,
                    "createMethodRequest",
                    "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
                temp.visitInsn(Opcodes.DUP);
                temp.visitVarInsn(Opcodes.ASTORE, requestLocal);
                visitInvokeHelperMethod(
                    temp,
                    "enterCoroutine",
                    "(ILkotlin/coroutines/Continuation;Ljava/lang/Object;)"
                        + Type.getDescriptor(Context.class));
                temp.visitInsn(Opcodes.DUP);
                temp.visitVarInsn(Opcodes.ASTORE, contextLocal);
                visitInvokeHelperMethod(
                    temp,
                    "openScope",
                    "("
                        + Type.getDescriptor(Context.class)
                        + ")"
                        + Type.getDescriptor(Scope.class));
                temp.visitVarInsn(Opcodes.ASTORE, scopeLocal);
                // @SpanAttribute handling
                for (Parameter parameter : annotatedParameters) {
                  // label on stack, make a copy
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitLdcInsn(parameter.name);
                  temp.visitVarInsn(parameter.type.getOpcode(Opcodes.ILOAD), parameter.var);
                  boolean primitive =
                      parameter.type.getSort() != Type.ARRAY
                          && parameter.type.getSort() != Type.OBJECT;
                  visitInvokeHelperMethod(
                      temp,
                      "setSpanAttribute",
                      "(ILjava/lang/String;"
                          + (primitive ? parameter.type.getDescriptor() : "Ljava/lang/Object;")
                          + ")V");
                }
                // pop label
                temp.visitInsn(Opcodes.POP);
                if (hasBlockingOperation) {
                  methodNode.instructions.insert(insertAfterInsn, temp.instructions);
                } else {
                  methodNode.instructions.insertBefore(
                      methodNode.instructions.get(0), temp.instructions);
                }
              }

              // insert at the start of the method
              // null the local variables we added
              // this is needed because jvm requires that a value needs to be assigned to the local
              // before it is used, we need to initialize the locals that we use in the exception
              // handler
              // if the previous block was added at the start of the method this nulling step isn't
              // necessary
              if (hasBlockingOperation) {
                MethodNode temp = new MethodNode();
                temp.visitInsn(Opcodes.ACONST_NULL);
                temp.visitVarInsn(Opcodes.ASTORE, requestLocal);
                temp.visitInsn(Opcodes.ACONST_NULL);
                temp.visitVarInsn(Opcodes.ASTORE, ourContinuationLocal);
                temp.visitInsn(Opcodes.ACONST_NULL);
                temp.visitVarInsn(Opcodes.ASTORE, contextLocal);
                temp.visitInsn(Opcodes.ACONST_NULL);
                temp.visitVarInsn(Opcodes.ASTORE, scopeLocal);

                methodNode.instructions.insertBefore(
                    methodNode.instructions.get(0), temp.instructions);
              }

              // insert exception handler code, this exception handler will catch Throwable
              {
                MethodNode temp = new MethodNode();
                // lastLocal is the last local we added before the start of try block
                int numLocals = lastLocal + 1;
                Object[] locals = new Object[numLocals];
                // in this handler we are using only the locals we added, we don't care about method
                // arguments and this, so we don't list them in the stack frame
                Arrays.fill(locals, Opcodes.TOP);
                locals[requestLocal] = Type.getInternalName(Object.class);
                locals[ourContinuationLocal] = Type.getInternalName(Continuation.class);
                locals[contextLocal] = Type.getInternalName(Context.class);
                locals[scopeLocal] = Type.getInternalName(Scope.class);

                temp.visitFrame(
                    Opcodes.F_NEW, numLocals, locals, 1, new Object[] {"java/lang/Throwable"});
                // we have throwable on stack
                // insert AnnotationInstrumentationHelper.exitCoroutine(exception, null, request,
                // context, scope)
                // that will close the scope and end span
                temp.visitInsn(Opcodes.DUP);
                temp.visitInsn(Opcodes.ACONST_NULL);
                temp.visitVarInsn(Opcodes.ALOAD, requestLocal);
                temp.visitVarInsn(Opcodes.ALOAD, ourContinuationLocal);
                temp.visitVarInsn(Opcodes.ALOAD, contextLocal);
                temp.visitVarInsn(Opcodes.ALOAD, scopeLocal);
                visitInvokeHelperMethod(
                    temp,
                    "exitCoroutine",
                    "(Ljava/lang/Throwable;Ljava/lang/Object;"
                        + "Ljava/lang/Object;"
                        + Type.getDescriptor(Continuation.class)
                        + Type.getDescriptor(Context.class)
                        + Type.getDescriptor(Scope.class)
                        + ")V");

                // rethrow the exception
                temp.visitInsn(Opcodes.ATHROW);

                methodNode.instructions.add(temp.instructions);
              }

              // insert code before each return instruction
              // iterating instructions in reverse order to avoid having to deal with the
              // instructions that we just added
              for (int i = methodNode.instructions.size() - 1; i >= 0; i--) {
                AbstractInsnNode instruction = methodNode.instructions.get(i);
                // this method returns Object, so we don't need to handle other return instructions
                if (instruction.getOpcode() == Opcodes.ARETURN) {
                  MethodNode temp = new MethodNode();
                  // we have return value on stack
                  // insert AnnotationInstrumentationHelper.exitCoroutine(returnValue, request,
                  // context, scope)
                  // that will close the scope and end span if needed
                  temp.visitInsn(Opcodes.DUP);
                  temp.visitVarInsn(Opcodes.ALOAD, requestLocal);
                  temp.visitVarInsn(Opcodes.ALOAD, ourContinuationLocal);
                  temp.visitVarInsn(Opcodes.ALOAD, contextLocal);
                  temp.visitVarInsn(Opcodes.ALOAD, scopeLocal);
                  visitInvokeHelperMethod(
                      temp,
                      "exitCoroutine",
                      "(Ljava/lang/Object;"
                          + "Ljava/lang/Object;"
                          + Type.getDescriptor(Continuation.class)
                          + Type.getDescriptor(Context.class)
                          + Type.getDescriptor(Scope.class)
                          + ")V");
                  methodNode.instructions.insertBefore(instruction, temp.instructions);
                }
              }

              methodNode.accept(target);
            }
          };

      return generatorAdapter;
    }

    private static void visitInvokeHelperMethod(
        MethodNode methodNode, String methodName, String descriptor) {
      methodNode.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          Type.getInternalName(AnnotationInstrumentationHelper.class),
          methodName,
          descriptor,
          false);
    }
  }
}
