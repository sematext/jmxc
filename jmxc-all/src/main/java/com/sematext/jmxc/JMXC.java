package com.sematext.jmxc;

import sun.management.ConnectorAddressLink;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * 
 * Surprisingly, JConsole is not really a 'console' utility. 
 * So, JMXC(onsole) is very simple 'console' utility to dump jmx data. 
 * Can connect to to JVM simple by pid (if run on the same machine), or via by URL ("jmx:rmi").
 * 
 * Currently not support some complex JMX datatypes.
 * 
 * @author sematext, http://www.sematext.com/
 */
public class JMXC {

  /**
   * 
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      error("Specify the pid or connection URL as first param, jmx query as second (optional)");
      return;
    }
    String connectTo = args[0];
    String pattern = args.length > 1 ? args[1] : null;

    run(connectTo, pattern);
  }

  private static int run(String connectTo, String pattern) throws Exception {
    Integer pid = pid(connectTo);
    String url = pid != null ? computeJMXServiceURL(pid) : connectTo;

    out("Connect via URL -> " + url);
    JMXConnector connector;
    try {
      connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
    } catch (NullPointerException e) {
      return error("Can't connect to pid -> " + pid
          + " ,try to run target JVM with 'com.sun.management.jmxremote' param");
    }
    
    if (connector == null) {
      return error("Sorry, can't connect to -> " + url);
    }
    MBeanServerConnection connection = connector.getMBeanServerConnection();

    String objectPattern = pattern != null ? pattern : "*:*";
    out("Will use '" + objectPattern + "' as pattern");
    Set<ObjectName> objectNames = new TreeSet<ObjectName>(connection.queryNames(new ObjectName(objectPattern), null));

    for (ObjectName objectName : objectNames) {
      out(objectName);
      MBeanInfo mbeanInfo = connection.getMBeanInfo(objectName);
      for (MBeanAttributeInfo attribute : mbeanInfo.getAttributes()) {
        String val = toString(read(connection, objectName, attribute));
        out("      " + attribute.getName() + " " + attribute.getType() + " = " + val);
      }
    }

    return 0;
  }

  private static Object read(MBeanServerConnection connection, ObjectName objectName, MBeanAttributeInfo attribute) {
    try {
      return attribute.isReadable() ? connection.getAttribute(objectName, attribute.getName()) : "#NON_READABLE";
    } catch (Exception e) {
      System.err.println(e);
    }
    return "#ERROR";
  }

  private static String toString(Object object) {
    if (object == null) {
      return null;
    }
    if ((object instanceof Object[])) {
      return Arrays.deepToString((Object[]) (Object[]) object);
    }
    return object.toString();
  }

  private static int error(Object out) {
    out(out);
    return -1;
  }

  private static void out(Object out) {
    System.out.println(out);
  }

  private static Integer pid(String nonParsed) {
    boolean isPid = nonParsed.matches("\\d+");
    return isPid ? Integer.parseInt(nonParsed) : null;
  }

  private static String computeJMXServiceURL(Integer pid) throws IOException {
    return ConnectorAddressLink.importFrom(pid);
  }
}
