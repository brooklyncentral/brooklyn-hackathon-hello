Brooklyn Hackathon Demo
=======================

### Building

With maven (v3) installed, and the code checked out, you can built an installable tarball using:

    mvn clean install
    cd hello-brooklyn/
    mvn assembly:assembly

Then unpack `hello-brooklyn/target/hello-brooklyn-1.0.0-SNAPSHOT-bin.tar.gz`.

You can also open the projects in your favourite IDE and run the class `HelloWebappClusterDatabaseApp`.
For more information on using IDE's, visit


### Cloud Credentials

To run, you'll need to specify credentials for your preferred cloud.  This can be done 
in `~/.brooklyn/brooklyn.properties`:

    brooklyn.jclouds.aws-ec2.identity=AKXXXXXXXXXXXXXXXXXX
    brooklyn.jclouds.aws-ec2.credential=secret01xxxxxxxxxxxxxxxxxxxxxxxxxxx
    brooklyn.location.named.aws-ec2-us-east-1=jclouds:aws-ec2:us-east-1
    brooklyn.location.named.aws-ec2-us-east-1.minRam=4096

Alternatively these can be set as shell environment parameters or JVM system properties.

Many other clouds are supported also, as well as pre-existing machines ("bring your own nodes"),
custom endpoints for private clouds, and specifying custom keys and passphrases.
For more information see:

    https://github.com/brooklyncentral/brooklyn/blob/master/docs/use/guide/defining-applications/common-usage.md#off-the-shelf-locations


### Run

Usage:

    ./start.sh [--port 8081+] location

Where location might be `localhost` (the default), or `named:aws-ec2-us-east-1`, `openstack:http://myendpoint`, etc.

Your application will be starting, with info printed in the console.  To follow it more closely, point
your browser to  http://localhost:8081/  (or higher port or that specified with `--port`).

Fire up a load-gen tool like jmeter, and watch it scale out.


### What's Next?

Stick a Geo-sensitivie load-balancing service in front of it, and have an application which spans
multiple geographies, or span your hand-rolled environment with Cloud Foundry and OpenShift, and
put the right elasticity policies in front of it.  See the brooklyn-social-apps like Drupal and
Wordpress, or Cloudera Hadoop (brooklyn-cdh) and MapR (brooklyn-mapr).

Or if you're more adventurous, pick an app that you care about and automate its deployment and 
management with Brooklyn.  Help create new entities -- from NoSQL to PHP -- and new policies and 
new topologies including advanced networking.

Visit  http://brooklyncentral.github.com  for more information.

