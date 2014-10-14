import AssemblyKeys._

name := "sfprolog"

version := "1.0.0"

initialCommands := """
"""

libraryDependencies += "jline" % "jline" % "2.12"

assemblySettings

assembleArtifact in packageScala := false

jarName in assembly := "prolog.jar"
