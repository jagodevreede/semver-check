package io.github.jagodevreede.semver.check.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyExampleClass {
   // Use a logger from the class path:
   private final static Logger log = LoggerFactory.getLogger(MyExampleClass.class);

   public boolean doImportantStuff() {
      log.trace("Done with important stuff");

      return true;
   }

}
