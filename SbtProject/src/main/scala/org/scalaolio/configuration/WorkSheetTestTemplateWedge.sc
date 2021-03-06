import scala.util.{Failure, Success, Try}
//
import org.scalaolio.configuration.{ValueTypedMap, ValueTyped, Transform, Nexus}
import org.scalaolio.java.lang.String_._
//
val tryNexusClean =
  Nexus.tryApply(
      "WorkSheetTestTemplateWedge"
    , List(
          "COMMAND_LINE_ARGS"
        , "JVM_CLASSPATH_CONFIGURATION"
        , "SERVER_APP_CONF"
        , "DATA_STORE"
      )
    //, templateProfile = None
      , optionValueWedgeNonEmptyTemplateExceptionResolver =
          Some(
            (subset, wedgeKeyAbsolute, templateKey, wedgeValueSanTemplatePrefix) => {
              def recursive(prefixPathRemaining: List[String]): Option[String] =
                if (prefixPathRemaining.isEmpty)
                  None
                else {
                  val keyResolved =
                    s"${prefixPathRemaining.mkString(".")}.$templateKey"
                  val optionValue =
                    subset.nexus.currentTransformDateTimeStamped.transform.valueTypedMap.valueByKey.get(keyResolved)
                  if (optionValue.isDefined)
                    optionValue
                  else
                    recursive(prefixPathRemaining.reverse.tail.reverse)
                }
              recursive(wedgeKeyAbsolute.splitLiterally("."))
            }
          )
  )
def tryAddOrUpdateToNexus(
    tryNexus: Try[Nexus]
  , keyAndValuesPrefixed: List[(String, String)]
  , sourceName: String
): Try[Nexus] =
  for {
    nexus <-
      tryNexus
    transform <-
    ValueTypedMap.tryApply(keyAndValuesPrefixed.toMap).map(Transform(_))
    transformNamed <-
      Nexus.TransformNamed.tryApply(transform, sourceName)
    nexusNew <-
      nexus.tryAddOrReplace(transformNamed)
  } yield nexusNew
val tryNexusAddedCommandLineArgs =
  tryAddOrUpdateToNexus(
      tryNexusClean
    , List(
          "commandlineargs.host.name" -> "aliAws01"
        , "commandlineargs.host.ip" -> "127.0.0.1"
        , "merged.host.name" -> "aliAws01"
        , "merged.host.ip" -> "127.0.0.1"
      )
    , "COMMAND_LINE_ARGS"
  )
val tryNexusAddedServerAppConf =
  tryAddOrUpdateToNexus(
    tryNexusClean
    , List(
        "serverappconf.akka.service.saas.name" -> "service-saas"
      , "serverappconf.akka.service.saas.interface" -> "template=>[host.ip]"
      , "serverappconf.akka.service.saas.port" -> "8443"
      , "merged.akka.service.saas.name" -> "service-saas"
      , "merged.akka.service.saas.interface" -> "template=>[host.ip]"
      , "merged.akka.service.saas.port" -> "8443"
    )
    , "SERVER_APP_CONF"
  )
val tryNexusAddedDataStore =
  tryAddOrUpdateToNexus(
    tryNexusAddedServerAppConf
    , List(
        "datastore.akka.spray.ssl.enabledCipherSuites" -> "0,1,2,3,1"
      , "datastore.akka.spray.ssl.enabled" -> "true"
      , "datastore.akka.spray.ssl.playerByJerseyNumber" -> "66->Jim.O'Flaherty,65->Mel.O'Flaherty,10->Brian.Score"
      , "merged.akka.spray.ssl.enabledCipherSuites" -> "0,1,2,3,1"
      , "merged.akka.spray.ssl.enabled" -> "true"
      , "merged.akka.spray.ssl.playerByJerseyNumber" -> "66->Jim.O'Flaherty,65->Mel.O'Flaherty,10->Brian.Score"
    )
    , "DATA_STORE"
  )
def toMap(tryNexus: Try[Nexus], keyPrefix: String = "", retainKeyPrefix: Boolean = false): Try[Map[String, String]] =
  for {
    nexus <-
      tryNexus
    subsetRoot <-
      nexus.trySubset(keyPrefix, retainKeyPrefix)
  } yield subsetRoot.toMap
val tryContent =
  for {
    valueByKey <-
      toMap(tryNexusAddedDataStore)
  } yield {
    for {
      (key, value) <-
        valueByKey
    } yield key + " -> " + (if (key.toLowerCase.endsWith(".password")) "********" else value)
  }
val contentAsString =
  tryContent match {
    case Success(lines) =>
      lines.toList.sorted.mkString("\n")
    case Failure(e) =>
      s"failed to produce content - ${e.getMessage}"
  }
implicit def parser: String => Try[Option[String]] =
  ValueTyped.Parsers.Classes.Singles.parseString
implicit def parserMapIntString: (String, String) => Try[Option[(Int, (String, String))]] =
  (stringJerseyNumber, stringPlayerName) =>
    ValueTyped.Parsers.Primitives.parseInt(stringJerseyNumber).flatMap(
      optionIntJerseyNumber =>
        Success(optionIntJerseyNumber.map((_, stringPlayerName.spanSansSeparator("."))))
    )
val fetchList =
  for {
    nexus <-
      tryNexusAddedDataStore
    subsetAkkaSpraySsl <-
      nexus.trySubset("merged.akka.spray.ssl.")
    optionEnabledCipherSuitesList <-
      subsetAkkaSpraySsl.valueTyped.Classes.Collections.tryOptionList("enabledCipherSuites")
    optionEnabledCipherSuitesSet <-
      //subsetAkkaSpraySsl.valueTyped.Classes.Collections.tryOptionSet("enabledCipherSuites")
      subsetAkkaSpraySsl.valueTyped.Classes.Collections.tryOptionSet[Int]("enabledCipherSuites")(ValueTyped.Parsers.Primitives.parseInt)
    optionPlayerByJerseyNumberMap <-
      subsetAkkaSpraySsl.valueTyped.Classes.Collections.tryOptionMap("playerByJerseyNumber")
    optionEnabled <-
      subsetAkkaSpraySsl.valueTyped.Primitives.tryOptionBoolean("enabled")//(ValueTyped.Parsers.Primitives.parseBoolean)
    subsetAkkaServiceSaas <-
      nexus.trySubset("merged.akka.service.saas.")
    optionPort <-
      subsetAkkaServiceSaas.valueTyped.Primitives.tryOptionInt("port")
  } yield (optionEnabledCipherSuitesList, optionEnabledCipherSuitesSet, optionPlayerByJerseyNumberMap, optionEnabled, optionPort)
    //subset.valueTyped.tryList[String]("enabledCipherSuites")
    //subset.valueTyped.tryList("enabledCipherSuites")(parse)
