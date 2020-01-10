
package ch.qos.logback.access.pattern;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogConstants;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SyslogStartConverter extends AccessConverter {

  // No severity per se for access logs, always using INFO messages
  public static final int SYSLOG_ACCESS_SEVERITY = SyslogConstants.ERROR_SEVERITY;

  private long lastTimestamp = -1;
  private String timestampStr = null;
  private SimpleDateFormat simpleMonthFormat;
  private SimpleDateFormat simpleTimeFormat;
  private final Calendar calendar = Calendar.getInstance(Locale.US);

  private String localHostName;
  private int facility;

  public void start() {
    int errorCount = 0;

    String facilityStr = getFirstOption();
    if (facilityStr == null) {
      addError("was expecting a facility string as an option");
      return;
    }

    facility = SyslogAppenderBase.facilityStringToint(facilityStr);

    localHostName = getLocalHostname();
    try {
      // hours should be in 0-23, see also http://jira.qos.ch/browse/LBCLASSIC-48
      simpleMonthFormat = new SimpleDateFormat("MMM", Locale.US);
      simpleTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    } catch (IllegalArgumentException e) {
      addError("Could not instantiate SimpleDateFormat", e);
      errorCount++;
    }

    if (errorCount == 0) {
      super.start();
    }
  }

  public String convert(IAccessEvent event) {

    int pri = facility + SYSLOG_ACCESS_SEVERITY;

    return "<"
        + pri
        + ">"
        + computeTimeStampString(event.getTimeStamp())
        + ' '
        + localHostName
        + ' ';
  }

  private String getLocalHostname() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (UnknownHostException uhe) {
      addError("Could not determine local host name", uhe);
      return "UNKNOWN_LOCALHOST";
    }
  }

  private String computeTimeStampString(long now) {
    synchronized (this) {
      // Since the formatted output is only precise to the second, we can use the same cached string if the
      // current
      // second is the same (stripping off the milliseconds).
      if ((now / 1000) != lastTimestamp) {
        lastTimestamp = now / 1000;
        Date nowDate = new Date(now);
        calendar.setTime(nowDate);
        timestampStr = String
            .format("%s %2d %s", simpleMonthFormat.format(nowDate), calendar.get(Calendar.DAY_OF_MONTH),
                simpleTimeFormat.format(nowDate));
      }
      return timestampStr;
    }
  }
}
