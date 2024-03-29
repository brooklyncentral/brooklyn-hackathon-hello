package brooklyn.hackathon.hello;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static brooklyn.event.basic.DependentConfiguration.formatString;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.enricher.HttpLatencyDetector;
import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.enricher.basic.SensorTransformingEnricher;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.JavaEntityMethods;
import brooklyn.entity.proxy.AbstractController;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.DynamicWebAppCluster;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.WebAppServiceConstants;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.basic.PortRanges;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.CommandLineUtil;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * Launches a 3-tier app with nginx, clustered jboss, and mysql.
 * <p>
 * Includes some advanced features such as KPI / derived sensors,
 * and annotations for use in a catalog.
 **/
public class HelloWebappClusterDatabaseApp extends AbstractApplication implements StartableApplication {
    
    public static final Logger LOG = LoggerFactory.getLogger(HelloWebappClusterDatabaseApp.class);
    
    public static final String DEFAULT_LOCATION = "localhost";

    public static final String DEFAULT_WAR_PATH = "classpath://brooklyn-hackathon-hello-webapp.war";
    
    @CatalogConfig(label="WAR (URL)", priority=2)
    public static final ConfigKey<String> WAR_PATH = new BasicConfigKey<String>(String.class,
        "app.war", "URL to the application archive which should be deployed", 
        DEFAULT_WAR_PATH);
    
    public static final String DEFAULT_DB_SETUP_SQL_URL = "classpath://visitors-creation-script.sql";
    
    @CatalogConfig(label="DB Setup SQL (URL)", priority=1)
    public static final ConfigKey<String> DB_SETUP_SQL_URL = new BasicConfigKey<String>(String.class,
        "app.db_sql", "URL to the SQL script to set up the database", 
        DEFAULT_DB_SETUP_SQL_URL);
    
    public static final String DB_TABLE = "visitors";
    public static final String DB_USERNAME = "brooklyn";
    public static final String DB_PASSWORD = "br00k11n";
    
    BasicAttributeSensor<Integer> APPSERVERS_COUNT = new BasicAttributeSensor<Integer>(Integer.class, 
            "appservers.count", "Number of app servers deployed");
    public static final AttributeSensor<Double> REQUESTS_PER_SECOND_IN_WINDOW = 
            WebAppServiceConstants.REQUESTS_PER_SECOND_IN_WINDOW;
    public static final AttributeSensor<String> ROOT_URL = WebAppServiceConstants.ROOT_URL;

    @Override
    public void init() {
        MySqlNode mysql = addChild(
                EntitySpecs.spec(MySqlNode.class)
                        .configure(MySqlNode.CREATION_SCRIPT_URL, getConfig(DB_SETUP_SQL_URL)));

        ControlledDynamicWebAppCluster web = addChild(
                EntitySpecs.spec(ControlledDynamicWebAppCluster.class)
                        .configure(WebAppService.HTTP_PORT, PortRanges.fromString("8080+"))
                        // to run on port 80:
//                        .configure(AbstractController.PROXY_HTTP_PORT, 80)
                        .configure(JavaWebAppService.ROOT_WAR, getConfig(WAR_PATH))
                        .configure(JavaEntityMethods.javaSysProp("brooklyn.example.db.url"), 
                                formatString("jdbc:%s%s?user=%s\\&password=%s", 
                                        attributeWhenReady(mysql, MySqlNode.MYSQL_URL), DB_TABLE, DB_USERNAME, DB_PASSWORD))
                        .configure(DynamicCluster.INITIAL_SIZE, 2)
                    );

        web.addEnricher(HttpLatencyDetector.builder().
                url(ROOT_URL).
                rollup(10, TimeUnit.SECONDS).
                build());
        
        web.getCluster().addPolicy(AutoScalerPolicy.builder().
                metric(DynamicWebAppCluster.REQUESTS_PER_SECOND_IN_WINDOW_PER_NODE).
                metricRange(10, 100).
                sizeRange(2, 5).
                build());

        addEnricher(SensorPropagatingEnricher.newInstanceListeningTo(web,  
                WebAppServiceConstants.ROOT_URL,
                DynamicWebAppCluster.REQUESTS_PER_SECOND_IN_WINDOW,
                HttpLatencyDetector.REQUEST_LATENCY_IN_SECONDS_IN_WINDOW));
        addEnricher(new SensorTransformingEnricher<Integer,Integer>(web, 
                DynamicWebAppCluster.GROUP_SIZE, APPSERVERS_COUNT, Functions.<Integer>identity()));
    }
    
    public static void main(String[] argv) {
        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION);

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                 .application(EntitySpecs.appSpec(HelloWebappClusterDatabaseApp.class)
                         .displayName("Hello Webapp Cluster"))
                 .webconsolePort(port)
                 .location(location)
                 .start();
             
        Entities.dumpInfo(launcher.getApplications());
    }
}
