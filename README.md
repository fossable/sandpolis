[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Sandpolis** is an advanced remote control system for both casual administrators and power users. It has all of the features you would expect from an administration tool - and also some you might not.

### Building Sandpolis
Building Sandpolis is as easy as running `./gradlew build` from the root project.

Alternatively, you can run `./gradlew eclipse` to generate project files and then import the root directory into Eclipse (be sure to enable "Search for nested projects" to import the submodules).

### A Brief History of Sandpolis
Sandpolis evolved out of a project called Crimson which was initially released in 2013. Crimson gained some popularity, but had serious design flaws and eventually became unmaintainable.

After four years of sporadic development, Crimson was officially abandoned and Sandpolis was born from the ashes. Some of the Crimson codebase was reused in the new project, but the majority of Sandpolis is brand new and now uses the most modern libraries and development practices.

<p align="center">
	<img src="https://raw.githubusercontent.com/Subterranean-Security/Sandpolis/4.X.X/screenshot.png"/><br>
	<font size="2">An early 2017 screenshot of Crimson</font>
</p>

Here are some major differences between Crimson and Sandpolis:

|              |       Crimson      | Sandpolis |
|--------------|:------------------:|:---------:|
| GUI          | Swing              | JavaFX    |
| Build Tool   | Ant                | Gradle    |
| Storage      | Java Serialization | Hibernate |
| Networking   | Netty              | Netty     |
| Unit Testing | None :)            | JUnit 5   |