#j2js-compiler

A Java Bytecode to JavaScript Cross-Compiler.

##Installation

1. You need to install this and the following projects

* https://github.com/decatur/j2js-compiler
* https://github.com/decatur/j2js-jre
* https://github.com/decatur/j2js-agent

2. Build all projects. An Eclipse project description is included.

3. Optionally install https://github.com/decatur/j2js-demos

##Usage

    java -cp <RUNTIME_CLASSPATH> com.j2js.J2JSCompiler <basedir> <CROSS_COMPILE_CLASSPATH> <entryPointClassName> <targetLocation>

###`<RUNTIME_CLASSPATH>`

This is the standard Java classpath.
The cross-compiler needs the project j2js-compiler, bcel and commons-io on its classpath. 

###`<basedir>`

All non-absolute paths are relative to the basedir.

###`<entryPointClassName>`

The name of the class to cross-compile. This class must have a method
`public void main(java.lang.String[])`.

The compiler cross-compiles the `main` method and all other methods which are called from the `main` method.

###`<CROSS_COMPILE_CLASSPATH>`

This classpath must contain all classes whose methods are referenced by the main method.
In normal operation, this classpath consists of
* the j2js-jre classes directory or jar
* the j2js-agent classes directory or jar
* the classes directory of your personal project

###`<targetLocation>`
All cross-compiled code is stored in the target location. It is one or more JavaScript file starting at
`0.js`. Only this initial file must be included in your web page with
`<script src='targetLocation/0.js'/>`.

###Example

Suppose the directory layout is

    |- j2js-compiler
    |- j2js-jre
    |- j2js-agent
    |- my-project
         |- target
             |- assemblies
                 |- 0.js
                 |- 1.js
                 |- 2.js ...

and you want to cross-compile class `org.mydomain.my-project.MyClass`. Then
    
    java -cp ../j2js-compiler/libs/commons-io-1.4.jar;^
    ../j2js-compiler/libs/bcel-5.1.jar;^
    ../j2js-compiler/target/classes ^
    com.j2js.J2JSCompiler . ^
    target/classes;../j2js-jre/target/classes;../j2js-agent/target/classes ^
    org.mydomain.my-project.MyClass target/assemblies

will create the assemblies.

##Warning
The project j2js-jre contains a hand-crafted version of the Java Runtime Environment. Do not substitute the path
to this project by a path to `Java/jreX/lib/rt.jar`. 

