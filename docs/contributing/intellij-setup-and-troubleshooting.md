# IntelliJ setup and troubleshooting

**NB!** Please ensure that Intellij uses the same java installation as you do for building this
project from command line. This ensures that Gradle task avoidance and build cache work properly and
can greatly reduce build time.

Suggested plugins and settings:

## [google-java-format](https://plugins.jetbrains.com/plugin/8527-google-java-format)

Installation:

![google format](https://user-images.githubusercontent.com/5099946/131758519-14d27c17-5fc2-4447-84b0-dbe7a7329022.png)

Configuration:

![enable google format](https://user-images.githubusercontent.com/5099946/131759832-36437aa0-a5f7-42c0-9425-8c5b45c16765.png)

Note: If google-java-format generates errors in Intellij,
see <https://github.com/google/google-java-format/issues/787#issuecomment-1200762464>.

## Load/Unload Modules

This project has many modules, which have many dependencies.
Therefore, IntelliJ indexes a lot and consumes a lot of CPU/memory resources.

To minimize IntelliJ's indexing and resource utilization,
[unload any modules](https://www.jetbrains.com/help/idea/unloading-modules.html)
on which you are not actively working.

Specifically, unload all modules, and then selectively load the modules on which you need to work.
IntelliJ will prompt you to load additional modules on which the selected modules depend.

If you are working on a specific instrumentation, you can load only the modules for that instrumentation.
For example, to load the modules for the Spring Boot autoconfigure instrumentation, run:

```shell
./docs/contributing/selectModules.kts instrumentation/spring/spring-boot-autoconfigure/
```

Install the [Kotlin executable](https://kotlinlang.org/docs/tutorials/command-line.html)
if you don't have it already.

## Debugging tests

See [Debugging tests](running-tests.md#debugging-tests) for more information on debugging tests.

## Troubleshooting

Occasionally, Intellij gets confused, maybe due to the number of modules in this project,
maybe due to other reasons. In any case, here's some things that might help:

### Invalidate Caches > "Just restart"

- Go to File > Invalidate Caches...
- Unselect all the options
- Click the "Just restart" link

This seems to fix more issues than just closing and re-opening Intellij :shrug:.

### Delete your `.idea` directory

- Close Intellij
- Delete the `.idea` directory in the root directory of your local repository
- Open Intellij
