//val spinalVersion = "1.9.4"
val spinalVersion = "dev"
val spinalHDLVersion = "latest.release"

val vexriscv = ProjectRef(file("third_party/VexRiscv/"), "root")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.tinyvision.ai",
      //scalaVersion := "2.11.12",
      scalaVersion := "2.12.18",
      //version      := "2.0.0"
      version      := "1.0.0"
    )),
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" % "spinalhdl-core_2.12" % spinalVersion,
      "com.github.spinalhdl" % "spinalhdl-lib_2.12" % spinalVersion,
      compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.12" % spinalVersion),
      //"org.scalatest" %% "scalatest" % "3.2.5",
      "org.scalatest" %% "scalatest" % "3.2.15",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
  ).dependsOn(vexriscv)

fork := true
