JythonServer
============

What is JythonServer?
---------------------

JythonServer is plugin that provide remote access to running Java application through Jython.

For instance, JythonServer can help you to alter value of static variable during runtime, such that you can change the behavior of application without restarting it.

Requirements:
-------------

Maven for building JythonServer.

JythonServer depends on jython-standalone, Maven should grap the jar automatically.

Installation:
-------------

To build JythonServer through Maven:

    mvn install

You will find jython-server.jar under target directory

Usage:
------

### Intergation to your application

To intergate JythonServer to your application, call

    JythonServer.singleton().startServer()

when your application is started.

The default port of the server is 5518. You may change it by providing the port number you want when you call startServer(). For example,

    JythonServer.singleton().startServer(1234)

You can also give a local variable maps to JythonServer through calling

    JythonServer.singleton().startServer(locals)

### Remote accessing JythonServer

There is a command line implementation of JythonServer client, you can start it with

    java -cp jython-server.jar com.hei.util.jython.jythonServer.JythonClient
