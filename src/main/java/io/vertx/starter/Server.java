package io.vertx.starter;

import com.cyngn.vertx.web.RestApi;
import com.cyngn.vertx.web.RouterTools;
import com.datastax.driver.core.Cluster;
import com.englishtown.vertx.cassandra.impl.DefaultCassandraSession;
import com.englishtown.vertx.cassandra.impl.JsonCassandraConfigurator;
import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.starter.rest.MessagesApi;
import io.vertx.starter.storage.cassandra.dal.MessagesDal;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.Thread;
import java.lang.Void;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GENERATED CODE DO NOT MODIFY - last updated: 2017-02-20T06:11:58.831Z
 * generated by exovert - https://github.com/cyngn/exovert
 *
 * Simple server that registers all {@link com.cyngn.vertx.web.RestApi} for CRUD operations.
 *
 * to build: ./gradlew clean shadowJar
 * to run: java -jar build/libs/[project-name]-fat.jar -conf [your_conf.json]
 */
public class Server extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  private static final String SHARED_DATA_KEY = "shared_data";

  private static final String INITIALIZER_THREAD_KEY = "initializer_thread";

  private LocalMap<String, Long> sharedData;

  private HttpServer server;

  private DefaultCassandraSession session;

  private int port;

  @Override
  public void start(final Future<Void> startedResult) {
    JsonObject config = config();

    if(!config.containsKey("cassandra")) { stop(); }

    sharedData = vertx.sharedData().getLocalMap(SHARED_DATA_KEY);
    sharedData.putIfAbsent(INITIALIZER_THREAD_KEY, Thread.currentThread().getId());
    session = new DefaultCassandraSession(Cluster.builder(), new JsonCassandraConfigurator(vertx), vertx);
    port = config.getInteger("http.port", 80);

    if(isInitializerThread()) {
      try {
        logger.info("Starting up server... on ip: {} port: {}", InetAddress.getLocalHost().getHostAddress(), port);
      } catch(UnknownHostException ex) {
        logger.error("Failed to get host ip address, ex: ", ex);
        stop();
      }
    }

    startServer();
    startedResult.complete();
  }

  public boolean isInitializerThread() {
    return sharedData.get(INITIALIZER_THREAD_KEY) == Thread.currentThread().getId();
  }

  @SuppressWarnings("unchecked")
  private void buildApi(Router router) {
    RouterTools.registerRootHandlers(router, LoggerHandler.create());

    List<RestApi> apis = Lists.newArrayList(
      new MessagesApi(new MessagesDal(session))
    );

    for(RestApi api: apis) {
      api.init(router);
      if(isInitializerThread()) {api.outputApi(logger);}
    }
  }

  private void startServer() {
    server = vertx.createHttpServer();
    
    Router router = Router.router(vertx);
    
    router.route("/").handler(routingContext -> {
		HttpServerResponse response = routingContext.response();
		response.putHeader("content-type", "text/html").end("<h1>Service from Vertx Gradle Cansandra Starter</h1>");
	});
    
    this.session.onReady(result -> {
        if (result.failed()) {
        	logger.info("error:"+ result.cause());;
            return;
        }
        buildApi(router);       
    });
  
    server.requestHandler(router::accept);

    server.listen(port, "0.0.0.0", event ->  {
      if(event.failed()) {
        logger.error("Failed to start server, error: ", event.cause());
        stop();
      } else {
        logger.info("Thread: {} starting to handle request", Thread.currentThread().getId());
      }
    } );
  }

  @Override
  public void stop() {
    logger.info("Stopping the server.");
    try {
      this.session.close();
      if(server != null) { server.close(); }      
    } finally {
      //make sure only one thread tries to shutdown.
      Long shutdownThreadId = sharedData.putIfAbsent("shutdown", Thread.currentThread().getId());
      if(shutdownThreadId == null) {
        vertx.close(event -> {
          logger.info("Vertx shutdown");
          System.exit(-1);
        } );
      }
    }
  }
}
