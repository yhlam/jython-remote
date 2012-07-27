JythonRemote
============

What is JythonRemote?
---------------------

JythonRemote is plugin that provide remote access to running Java application through Jython.

For instance, JythonRemote can help you to alter value of static variable during runtime, such that you can change the behavior of application without restarting it.

Requirements:
-------------

Maven for building JythonRemote.

JythonRemote depends on jython-standalone, Maven should grap the jar automatically.

Installation:
-------------

To build JythonRemote through Maven:

    mvn install

You will find jython-remote.jar under target directory

Usage:
------

### Intergation to your application

To intergate JythonRemote to your application, call

    JythonRemoteServer.singleton().startServer()

when your application is started.

The default port of the server is 5518. You may change it by providing the port number you want when you call startServer(). For example,

    JythonRemoteServer.singleton().startServer(1234)

You can also give a local variable maps to JythonRemote through calling

    JythonRemoteServer.singleton().startServer(locals)

### Remote accessing JythonRemote

There is a command line implementation of JythonRemote client, you can start it with

    java -cp jython-remote.jar com.hei.util.jython.JythonRemote.JythonRemoteClient
