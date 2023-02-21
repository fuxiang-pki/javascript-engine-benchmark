package org.example;

import com.eclipsesource.v8.V8;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.Invocable;
import java.io.IOException;
import org.graalvm.polyglot.Source;

/**
 * Simple benchmark for Graal.js via GraalVM Polyglot Context and ScriptEngine.
 */
public class App {

  public static final int WARMUP = 30;
  public static final int ITERATIONS = 10;
  public static final String SOURCE = """
      var N = 2000;
      var EXPECTED = 17393;

      function Natural() {
          x = 2;
          return {
              'next' : function() { return x++; }
          };
      }

      function Filter(number, filter) {
          var self = this;
          this.number = number;
          this.filter = filter;
          this.accept = function(n) {
            var filter = self;
            for (;;) {
                if (n % filter.number === 0) {
                    return false;
                }
                filter = filter.filter;
                if (filter === null) {
                    break;
                }
            }
            return true;
          };
          return this;
      }

      function Primes(natural) {
          var self = this;
          this.natural = natural;
          this.filter = null;

          this.next = function() {
              for (;;) {
                  var n = self.natural.next();
                  if (self.filter === null || self.filter.accept(n)) {
                      self.filter = new Filter(n, self.filter);
                      return n;
                  }
              }
          };
      }

      function primesMain() {
          var primes = new Primes(Natural());
          var primArray = [];
          for (var i=0;i<=N;i++) { primArray.push(primes.next()); }
          if (primArray[N] != EXPECTED) { throw new Error('wrong prime found: '+primArray[N]); }
      }
      """;

  public static void main(String[] args) throws Exception {
    benchGraalPolyglotContext();
    benchGraalScriptEngine();
    benchNashornScriptEngine();
    benchJ2V8();
  }

  static long benchGraalPolyglotContext() throws IOException {
    System.out.println("=== Graal.js via org.graalvm.polyglot.Context === ");
    long sum = 0;
    try (Context context = Context.create()) {
      context.eval(Source.newBuilder("js", SOURCE, "src.js").build());
      Value primesMain = context.getBindings("js").getMember("primesMain");
      System.out.println("warming up ...");
      for (int i = 0; i < WARMUP; i++) {
        primesMain.execute();
      }
      System.out.println("warmup finished, now measuring");
      for (int i = 0; i < ITERATIONS; i++) {
        long start = System.currentTimeMillis();
        primesMain.execute();
        long took = System.currentTimeMillis() - start;
        sum += took;
        System.out.println("iteration: " + took + " ms");
      }
    } // context.close() is automatic
    return sum;
  }

  static long benchJ2V8() {
    System.out.println("=== J2V8 via com.eclipsesource.v8.V8 runtime ===");
    V8 v8Runtime = V8.createV8Runtime();
    long sum = 0;
    try {
      System.out.println("warming up ...");
      v8Runtime.executeScript(SOURCE, "src.js", 0);
      for (int i = 0; i < WARMUP; i++) {
        v8Runtime.executeVoidFunction("primesMain", null);
      }
      System.out.println("warmup finished, now measuring");

      for (int i = 0; i < ITERATIONS; i++) {
        long start = System.currentTimeMillis();
        v8Runtime.executeVoidFunction("primesMain", null);
        long took = System.currentTimeMillis() - start;
        sum += took;
        System.out.println("iteration: " + took + " ms");
      }
    } finally {
      v8Runtime.release();
    }

    return sum;
  }

  static long benchNashornScriptEngine() throws IOException {
    System.out.println("=== Nashorn via javax.script.ScriptEngine ===");
    ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
    if (nashornEngine == null) {
      System.out.println("*** Nashorn not found ***");
      return 0;
    } else {
      return benchScriptEngineIntl(nashornEngine);
    }
  }

  static long benchGraalScriptEngine() throws IOException {
    System.out.println("=== Graal.js via javax.script.ScriptEngine ===");
    ScriptEngine graaljsEngine = new ScriptEngineManager().getEngineByName("graal.js");
    if (graaljsEngine == null) {
      System.out.println("*** Graal.js not found ***");
      return 0;
    } else {
      return benchScriptEngineIntl(graaljsEngine);
    }
  }

  private static long benchScriptEngineIntl(ScriptEngine eng) throws IOException {
    long sum = 0L;
    try {
      eng.eval(SOURCE);
      Invocable inv = (Invocable) eng;
      System.out.println("warming up ...");
      for (int i = 0; i < WARMUP; i++) {
        inv.invokeFunction("primesMain");
      }
      System.out.println("warmup finished, now measuring");
      for (int i = 0; i < ITERATIONS; i++) {
        long start = System.currentTimeMillis();
        inv.invokeFunction("primesMain");
        long took = System.currentTimeMillis() - start;
        sum += took;
        System.out.println("iteration: " + (took) + " ms");
      }
    } catch (Exception ex) {
      System.out.println(ex);
    }
    return sum;
  }

}