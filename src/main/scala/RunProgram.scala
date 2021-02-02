import java.io.File
import java.net.URL

import geotrellis.vector.Polygon
import org.fsm.processors.{CLHCProcessor, LHCConfig, SampleAlignProcessor}
import org.fsm.{DataFormat, GridDataFile, NDVIFile, Paddock, PointDataset}
import org.geotools.geojson.feature.FeatureJSON
import org.locationtech.jts.geom.GeometryFactory
import org.opengis.feature.simple.SimpleFeature
import play.api.libs.json.Json

object RunProgram{
  def main(args: Array[String]): Unit = {
    println("It is running")
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
    println(paddocks);
    println(paddocks.length);

    val lhcConfig = Option(LHCConfig(samples=4, metrics=Seq("Average NDVI", "Elevation"), perPaddock = false))
    val lhcRes = new CLHCProcessor().build(paddocks, lhcConfig)
    print(lhcRes)

  }

}
