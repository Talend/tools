# Code formatting

### Scalafmt
* Using [Scalafmt](http://scalameta.org/scalafmt/) standard
* Using the LucidCharts implementation : https://github.com/lucidsoftware/neo-sbt-scalafmt
* Declared as part of the data-streams-settings SBT plugin : https://github.com/Talend/data-streams-tools/blob/master/sbt-data-streams-settings/build.sbt
* Using default style with some minor modifications defined in the `.scalafmt.conf` in each project's root

### Usage
Resources can be found in the `scala-formatter` folder. This includes the scala formatter (`Scalafmt`) configuration file `scala-formatter/scalafmt.conf`.

* Copy the configuration file `scala-formatter/scalafmt.conf` as `.scalafmt.conf` to the root of your project.
*  Add `addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")` to your `project/plugins.sbt`
* Add the following settings `scalaFmtSettings` to your SBT project definition :

```java
/**
    * scalafmt configurations
    * https://github.com/lucidsoftware/neo-sbt-scalafmt#scalafmt-configuration
    */
  lazy val scalaFmtSettings = Seq(
    /**
      * run scalafmt automatically before compiling
      */
    com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile := true,
    /**
      * run scalafmt::test automatically before compiling
      */
    com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtTestOnCompile := true,
    /**
      * warn when scalafmt::test fails instead of failing
      */
    com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtFailTest := false
  )
```

The above settings instruct the plugin to reformat sources and tests upon compile.

### Useful commands
* format compile sources : `sbt scalafmt`
* format test sources : `sbt test:scalafmt`
* format SBT soruces : `sbt sbt:scalafmt`

To ensure everything is formatted, and fail if it is not (e.g. as a CI step) :
* check compile sources `sbt scalafmt::test`
* check test sources `sbt test:scalafmt::test`
* check .sbt sources `sbt sbt:scalafmt::test`

### Intellij plugin
https://plugins.jetbrains.com/plugin/8236-scalafmt

# Static code analysis

### Scala-style
* Using http://www.scalastyle.org/
* Using SBT plugin : http://www.scalastyle.org/sbt.html
* Rules defined in `scalastyle-config.xml` in each project's root

### Usage
Resources can be found in the `scala-style` folder. This includes the scala style (`Scala-style`) configuration file `scala-style/scalastyle-config.xml`.

* Copy the configuration file `scala-style/scalastyle-config.xml` as `scalastyle-config.xml` to the root of your project.
*  Add `addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")` to your `project/plugins.sbt`

### Useful commands
* run code analysis : `sbt scalastyle`
* run code analysis on test sources : `sbt test:scalastyle`

### Intellij plugin
Scalastyle is automatically supported in Intellij
