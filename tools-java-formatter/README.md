### IntelliJ idea
* Install [Eclipse Code Formatter](https://plugins.jetbrains.com/plugin/6546) plugin
* Import the java formatter file [src/main/resources/talend_java_eclipse_formatter.xml](src/main/resources/talend_java_eclipse_formatter.xml)
* Set the order import manually as followed `java;javax;org;com;`
* Disable import on the fly (Settings... -> Editor -> General -> Auto Import, uncheck Optimize imports on the fly)
* Disable `.*` imports (Settings... -> Editor -> Code Style -> Java -> Imports)
  - `Class count to use import with '*'` : 999
  - `Names count to use import static with '*'` : 999

That's it, you're good to go !

### Eclipse
* Open Eclipse preferences
* Select the Java->Code Style->Formatter section.
* click on the *Import* button and select the file  (src/main/resources/talend_java_eclipse_formatter.xml)

### Maven
#### setup formatter validation
To setup the automatic formatter validation for every maven build please add the following plugin definition to your pom.xml
```
<build>
	<plugins>
		<plugin>
			<groupId>net.revelc.code.formatter</groupId>
			<artifactId>formatter-maven-plugin</artifactId>
			<version>1.6.0-SNAPSHOT</version>
			<executions>
	          <execution>
	            <goals>
	              <goal>validate</goal>
	            </goals>
	          </execution>
	        </executions>
            <configuration>
            	<encoding>UTF-8</encoding>
                <configFile>talend_java_eclipse_formatter.xml</configFile>
            </configuration>
			<dependencies>
	          <dependency>
	            <groupId>org.talend.tools</groupId>
	            <artifactId>java-formatter</artifactId>
	            <version>0.1.0</version>
	          </dependency>
	        </dependencies>
		</plugin>
	</plugins>
</build>
```

You may have to add this plugin repository to you pom too if it is not already done.
```
	<pluginRepositories>
        <pluginRepository>
            <id>talend-opensource-snapshot</id>
            <name>talend-opensource-snapshot</name>
            <url>http://newbuild.talend.com:8081/nexus/content/repositories/TalendOpenSourceSnapshot/</url>
        </pluginRepository>
    </pluginRepositories>
```
If you want to format all your source code with the formatter config file you can change the goal from *validate* to *format*
