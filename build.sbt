name := "fsm-test"

version := "0.1"

//scalaVersion := "2.12.7"

scalaVersion in ThisBuild := "2.12.8"
lazy val fsm = project in file ("Modules/fsm-scala")
lazy val proj = (project in file(".")).dependsOn(fsm)
