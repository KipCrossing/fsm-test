import java.io.File
import java.net.URL

import geotrellis.vector.Polygon
import org.fsm.processors.{CLHCProcessor, DataGenerateProcessor, LHCConfig, SampleAlignProcessor}
import org.fsm.{DataFormat, GridDataFile, NDVIFile, Paddock, PointAttribute, PointDataset}
import org.geotools.geojson.feature.FeatureJSON
import org.locationtech.jts.geom.GeometryFactory
import org.opengis.feature.simple.SimpleFeature
import play.api.libs.json.Json
import com.typesafe.config.ConfigFactory

object RunProgram{
  def main(args: Array[String]): Unit = {
    println("It is running")

    org.fsm.config = ConfigFactory.parseResources("fsm.conf")


    val nirInput = new File(s"/home/kipling/Documents/fsm_sample_data/nir.tif").toURI().toURL()
    val nirData = GridDataFile(uid="nir", metricType="Average NDVI", file=nirInput, fileType=DataFormat.Tiff)


    val redInput = new File(s"/home/kipling/Documents/fsm_sample_data/red.tif").toURI().toURL()
    val redData = GridDataFile(uid="red", metricType="Average NDVI", file=redInput, fileType=DataFormat.Tiff)


    val elevationInput = new File(s"/home/kipling/Documents/fsm_sample_data/dem.tif").toURI().toURL()
    val elevationFile = GridDataFile(uid="dem_123", metricType="Elevation", file=elevationInput, fileType=DataFormat.Tiff)


    val ndviFile = NDVIFile(id="ndvi", red=redData, nir=nirData)


    val paddockJson = new FeatureJSON().readFeatureCollection(new File(s"/home/kipling/Documents/fsm_sample_data/paddocks.json").toURI().toURL().openStream())
    val samplePoints = Json.parse(new File(s"/home/kipling/Documents/fsm_sample_data/samples.json").toURI().toURL().openStream()).as[List[PointDataset]]

    val geomFactory = new GeometryFactory()

    val paddocks = paddockJson.toArray().flatMap{
      case sf:SimpleFeature if sf.getDefaultGeometry.isInstanceOf[Polygon] =>
        val bounds = sf.getDefaultGeometry.asInstanceOf[Polygon]
        println(bounds)
        val samplesInside = samplePoints.filter(s => bounds.contains(geomFactory.createPoint(s.location)))
        println(samplesInside)
        Some(
          Paddock(
            ndviFiles = List(ndviFile),
            otherGridFiles = List(elevationFile),
            bounds = bounds,
            soilPointDataArray = samplesInside,
            id = sf.getID
          )
        )
      case _ =>
        None
    }

    def PADDOCKS(ignoreGrid:Boolean = false) = {


      val p = paddocks.filter(_.soilPointDataArray.nonEmpty).map(p => p.copy(

        // remove any aligned gird values and AWC (as tests will need to make those)
        soilPointDataArray = p.soilPointDataArray.map(s => s.copy(
          alignedGridValues = None,
          attributes = s.attributes.filter(_.name != PointAttribute.AWC_RESULT)
        ))
      ))

      if(ignoreGrid || p.forall(_.getAllDataAsRDD(Set("Average NDVI","Elevation")).nonEmpty))
        p
      else{
        // run the grid generator as all tests need it
        new DataGenerateProcessor().build(p)
        p
      }

    }

//    val gridRes = new DataGenerateProcessor().build(PADDOCKS(true))


    println(paddocks);
    println(paddocks.length);

    val gridRes = new DataGenerateProcessor().build(paddocks)

    println(gridRes)

//    val lhcConfig = Option(LHCConfig(samples=4, metrics=Seq("Average NDVI", "Elevation"), perPaddock = false))
//    val lhcRes = new CLHCProcessor().build(paddocks, lhcConfig)
//    print(lhcRes)

  }

}
