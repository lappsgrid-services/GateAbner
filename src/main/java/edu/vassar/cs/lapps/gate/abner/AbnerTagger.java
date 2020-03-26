package edu.vassar.cs.lapps.gate.abner;

import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import org.anc.lapps.gate.serialization.GateSerializer;
import org.lappsgrid.api.WebService;
import org.lappsgrid.metadata.ServiceMetadataBuilder;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 *
 */
public class AbnerTagger implements WebService
{
	protected String metadata;
	protected final Logger logger = LoggerFactory.getLogger(AbnerTagger.class);
	protected String cachedError;
	protected AbstractLanguageAnalyser pr;
	private String annotationType;

	public AbnerTagger()
	{
		annotationType = "AbnerTagger";
		File plugins = null;
		if (!Gate.isInitialised())
		{
			logger.info("Initializing GATE.");
			// TODO Gate home needs to be parameterized.
			File home = new File("/usr/local/lapps/gate_abner");
			File siteConfig = new File(home, "gate.xml");
			File userConfig = new File(home, "user-gate.xml");
			plugins = new File(home, "plugins");
			Gate.setGateHome(home);
			Gate.setSiteConfigFile(siteConfig);
			Gate.setPluginsHome(plugins);
			Gate.setUserConfigFile(userConfig);
			try
			{
				Gate.init();
				CreoleRegister register = Gate.getCreoleRegister();
				for (String plugin : plugins.list())
				{
					logger.debug("Registering {}", plugin);
					File pluginDir = new File(plugins, plugin);
					register.registerDirectories(pluginDir.toURI().toURL());
				}
			}
			catch (Throwable e)
			{
				logger.error("Unable to initialize the AbnerTagger");
				cachedError = new Data(Uri.ERROR, e.getMessage()).asPrettyJson();
				return;
			}
		}
		try
		{
			pr = (AbstractLanguageAnalyser) Factory.createResource("gate.abner.AbnerTagger");
//			serializer = new GateSerializer();
			logger.debug("AbnerTagger has been initialized.");
		}
		catch (ResourceInstantiationException e)
		{
			logger.error("Unable to create the AbnerTagger.", e);
			cachedError = new Data(Uri.ERROR, e.getMessage()).asPrettyJson();
		}
	}

	public String getMetadata()
	{
		if (metadata == null) {
			metadata = new ServiceMetadataBuilder()
					.name(this.getClass().getName())
					.description("ABNER Biomedical NER")
					.license(Uri.GPL3)
					.version(Version.getVersion())
					.vendor("http://www.lappsgrid.org")
					.produce(Uri.NE)
					.requireFormats(Uri.GATE, Uri.LIF)
					.produceFormats(Uri.GATE, Uri.LIF)
					.toString();
		}
		return metadata;
	}

	public String execute(String input) {
		logger.info("Executing.");
		if (cachedError != null)
		{
			logger.warn("Returning cached error");
			return cachedError;
		}

		Data data = Serializer.parse(input, Data.class);
		String discriminator = data.getDiscriminator();
		if (Uri.ERROR.equals(discriminator))
		{
			logger.warn("Incoming data contained a Uri.ERROR discriminator.");
			return input;
		}

		boolean convert = false;
		Document document = null;
		if (Uri.GATE.equals(discriminator))
		{
			try
			{
				document = (Document)
						Factory.createResource("gate.corpora.DocumentImpl",
								Utils.featureMap(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME,
										data.getPayload(),
										Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/xml"));
			}
			catch (ResourceInstantiationException e)
			{
				return new Data(Uri.ERROR, e.getMessage()).asPrettyJson();
			}
		}
		else if (Uri.LIF.equals(discriminator))
		{
			if (!(data.getPayload() instanceof Map)) {
				return new Data(Uri.ERROR, "Invalid LIF document received.").asPrettyJson();
			}
			Container container = new Container((Map) data.getPayload());
			document = GateSerializer.convertToDocument(container);
			convert = true;
		}
		else {
			return new Data(Uri.ERROR, "Invalid discriminator. Expected " + Uri.GATE + " Found " + discriminator).asPrettyJson();
		}

		try
		{
			pr.setDocument(document);
			pr.setParameterValue("annotationName", annotationType);
			pr.execute();
			pr.setDocument(null);
			FeatureMap features = document.getFeatures();
			Integer step = (Integer) features.get("lapps:step");
			if (step == null) {
				step = 1;
			}
			features.put("lapps:step", step + 1);
			features.put("lapps:" + annotationType, step + " " + this.getClass().getName() + " gate");
//			AnnotationSet set = document.getAnnotations();
//			for (Annotation a : set.inDocumentOrder()) {
//				FeatureMap f = a.getFeatures();
//				if (f.containsKey("type")) {
//					String type = f.get("type").toString();
//					f.put(Features.NamedEntity.CATEGORY, type);
//					f.remove("type");
//				}
//			}
		}
		catch (ResourceInstantiationException | ExecutionException e)
		{
			return new Data(Uri.ERROR, e.getMessage()).asPrettyJson();
		}

		if (convert) {
			Container container = GateSerializer.convertToContainer(document);
			//
			return new Data(Uri.LIF, container).asPrettyJson();
		}
		return new Data(Uri.GATE, document.toXml()).asPrettyJson();
	}

}
