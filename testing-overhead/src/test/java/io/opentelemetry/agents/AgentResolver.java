/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.agents;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AgentResolver {

  private final LatestAgentSnapshotResolver snapshotResolver = new LatestAgentSnapshotResolver();

  public Optional<Path> resolve(Agent agent) throws Exception {
    if(Agent.NONE.equals(agent)){
      return Optional.empty();
    }
    if(Agent.LATEST_SNAPSHOT.equals(agent)){
      return snapshotResolver.resolve();
    }
    if(agent.hasUrl()){
      return Optional.of(downloadAgent(agent));
    }
    throw new IllegalArgumentException("Unknown agent: " + agent);
  }

  Path downloadAgent(Agent agent) throws Exception {
    if(agent.getUrl().getProtocol().equals("file")){
      Path source = Path.of(agent.getUrl().toURI());
      Path result = Paths.get(".", source.getFileName().toString());
      Files.copy(source, result);
      return result;
    }
    Request request = new Request.Builder().url(agent.getUrl()).build();
    OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();
    byte[] raw = response.body().bytes();
    Path path = Paths.get(".", "opentelemetry-javaagent-all.jar");
    FileOutputStream out = new FileOutputStream(path.toFile());
    out.write(raw);
    out.flush();
    out.close();
    return path;
  }

  // TODO: Download latest snapshot

}
