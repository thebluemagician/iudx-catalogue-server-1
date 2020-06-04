package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.onboarder.OnboarderService;
import iudx.catalogue.server.validator.ValidatorService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Catalogue Server API Verticle.
 * <h1>Catalogue Server API Verticle</h1>
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 * </p>
 * 
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */

public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ApiServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private DatabaseService database;
  private OnboarderService onboarder;
  private ValidatorService validator;
  private AuthenticationService authenticator;
  private HttpServer server;
  private Router router;
  private Properties properties;
  private InputStream inputstream;
  private final int port = 8443;
  private String keystore;
  private String keystorePassword;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {

    /** Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /** Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;

        /** Define the APIs, methods, endpoints and associated methods. */

        Router router = Router.router(vertx);
        router.route("/apis/*").handler(StaticHandler.create());

        /** Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          keystore = properties.getProperty("keystore");
          keystorePassword = properties.getProperty("keystorePassword");

        } catch (Exception ex) {

          logger.info(ex.toString());

        }

        /** Setup the HTTPs server properties, APIs and port. */

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(port);

        /** Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /** Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class,
            databaseServiceDiscoveryHandler -> {
              if (databaseServiceDiscoveryHandler.succeeded()) {
                database = databaseServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + database.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /** Get a handler for the OnboarderService from Service Discovery interface. */

        EventBusService.getProxy(discovery, OnboarderService.class,
            onboarderServiceDiscoveryHandler -> {
              if (onboarderServiceDiscoveryHandler.succeeded()) {
                onboarder = onboarderServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + onboarder.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /** Get a handler for the ValidatorService from Service Discovery interface. */

        EventBusService.getProxy(discovery, ValidatorService.class,
            validatorServiceDiscoveryHandler -> {
              if (validatorServiceDiscoveryHandler.succeeded()) {
                validator = validatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + validator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /** Get a handler for the AuthenticationService from Service Discovery interface. */

        EventBusService.getProxy(discovery, AuthenticationService.class,
            authenticatorServiceDiscoveryHandler -> {
              if (authenticatorServiceDiscoveryHandler.succeeded()) {
                authenticator = authenticatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + authenticator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

      }
    });


  }

}
