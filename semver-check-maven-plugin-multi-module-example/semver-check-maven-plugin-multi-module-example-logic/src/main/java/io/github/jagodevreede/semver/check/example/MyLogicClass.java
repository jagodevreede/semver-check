package io.github.jagodevreede.semver.check.example;

public class MyLogicClass {
   public boolean doImportantStuff() {
      // Delegate the call to the other module
      return new MyExampleClass().doImportantStuff();
   }

}
