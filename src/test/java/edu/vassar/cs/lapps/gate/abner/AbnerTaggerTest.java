package edu.vassar.cs.lapps.gate.abner;

import gate.creole.ResourceInstantiationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 *
 */
public class AbnerTaggerTest
{

	protected WebService tagger;

	@Before
	public void setup()
	{
		tagger = new AbnerTagger();
	}

	@After
	public void teardown()
	{
		tagger = null;
	}

	@Test
	public void getMetadata()
	{
		String json = tagger.getMetadata();
		System.out.println(json);
	}

	@Test
	public void executeGate() throws ResourceInstantiationException
	{
		String xml = loadData("/gate-doc.xml");
		Data data = new Data<String>(Uri.GATE, xml);
		String json = tagger.execute(data.asJson());
		data = Serializer.parse(json);
		System.out.println(data.getPayload().toString());
//		Document doc = Factory.newDocument(data.getPayload().toString());
//		Container container = GateSerializer.convertToContainer(doc);
//		System.out.println(Serializer.toPrettyJson(container));
	}

	@Test
	public void executeLif()
	{
		String txt = loadData("/sample.txt");
		Container container = new Container();
		container.setText(txt);
		container.setLanguage("en");
		Data<Container> data = new Data<>(Uri.LIF, container);
		String json = tagger.execute(data.asJson());
		System.out.println(json);
	}

	private String loadData(String path)
	{
		InputStream stream = this.getClass().getResourceAsStream(path);
		assertNotNull(stream);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringWriter writer = new StringWriter();
		return reader.lines().collect(Collectors.joining("\n"));
	}
}