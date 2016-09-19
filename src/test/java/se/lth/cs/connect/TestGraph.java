package se.lth.cs.connect;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrRelation;

public class TestGraph {

	@Test
	public void testEmptyGraph() {
		Graph empty = new Graph(new LinkedList<GrNode>(), new LinkedList<GrRelation>());
		
		assertEquals("No relations provided", empty.edges.length, 0);
		assertEquals("No nodes provided", empty.nodes.length, 0);
	}

}
