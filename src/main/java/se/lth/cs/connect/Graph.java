package se.lth.cs.connect;

import java.util.HashSet;
import java.util.List;

import iot.jcypher.graph.GrLabel;
import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrProperty;
import iot.jcypher.graph.GrRelation;

/**
 * Helper class for simplifying json reflection.
 *
 * Graph
 * Graph.Node
 * Graph.Edge
 * Graph.User
 * Graph.Collection
 */
public class Graph {
    public Node[] nodes;
    public Edge[] edges;

    public Graph(List<GrNode> entries, List<GrRelation> rels) {
        nodes = Node.fromList(entries);
        edges = Edge.fromList(rels);
    }

    public static class User {
        public String email;
        public int defaultCollection;
        public String trust;
        // public String signUpDate; // signupdate
        // no password for obvious reasons

        public User(GrNode base) {
            for (GrProperty prop : base.getProperties()) {
                switch (prop.getName()) {
                case "email":
                    email = prop.getValue().toString(); break;
                case "trust":
                    java.math.BigDecimal tbig = (java.math.BigDecimal)prop.getValue();
                    trust = TrustLevel.toString(tbig.intValue());
                    break;
                case "default":
                    java.math.BigDecimal big = (java.math.BigDecimal)prop.getValue();
                    defaultCollection = big.intValue();
                default: break;
                }
            }
        }

        public static User[] fromList(List<GrNode> matches) {
            HashSet found = new HashSet<User>(matches.size());
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) != null)
                    found.add(new User(matches.get(i)));
            }
            User[] users = new User[found.size()];
            found.toArray(users);
            return users;
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof User)) return false;
            return ((User)o).email.equals(email);
        }

        @Override
        public int hashCode(){
            return email.hashCode();
        }
    }

    public static class Collection {
        public String name;
        public long id;

        public Collection(GrNode base) {
            id = base.getId();
            for (GrProperty prop : base.getProperties()) {
                switch (prop.getName()) {
                case "name":
                    name = prop.getValue().toString(); break;
                default: break;
                }
            }
        }

        public static Collection[] fromList(List<GrNode> matches) {
            HashSet found = new HashSet<Collection>(matches.size());
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) != null)
                    found.add(new Collection(matches.get(i)));
            }
            Collection[] groups = new Collection[found.size()];
            found.toArray(groups);
            return groups;
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Collection)) return false;
            return ((Collection)o).id == id;
        }

        @Override
        public int hashCode(){
            return (int)id;
        }
    }

    public static class Node {
        public long id;
        public String hash, type, contact, reference, doi, description, date;
        public boolean pending;

        public Node(){
            id = 0; hash = ""; type = "";
        }

        public Node(GrNode base) {
            this.id = base.getId();

            for (GrProperty prop : base.getProperties()) {
                switch (prop.getName()) {
                case "hash":
                    hash = prop.getValue().toString(); break;
                case "contact":
                    contact = prop.getValue().toString(); break;
                case "reference":
                    reference = prop.getValue().toString(); break;
                case "doi":
                    doi = prop.getValue().toString(); break;
                case "description":
                    // neo4j escapes ' --> \'
                    description = prop.getValue().toString().replace("\\'", "'"); break;
                case "pending":
                    pending = (Boolean)prop.getValue(); break;
                case "date":
                	date = prop.getValue().toString(); break;
                default: break;
                }
            }

            for (GrLabel label : base.getLabels()) {
                switch (label.getName()) {
                case "research":
                case "challenge":
                    type = label.getName(); break;
                default: break;
                }
            }
        }

        @Override
        public int hashCode(){
            return (int)id;
        }

        @Override
        public boolean equals(Object obj) {
           if (!(obj instanceof Node))
                return false;

            if (obj == this)
                return true;

            return ((Node)obj).id == id;
        }

        /**
         * The json() method works best on POJOs, so transform a complex GrNode
         * list to a simple Node array.
         */
        public static Node[] fromList(List<GrNode> matches) {
            HashSet found = new HashSet<Node>(matches.size());
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) != null)
                    found.add(new Node(matches.get(i)));
            }
            Node[] nodes = new Node[found.size()];
            found.toArray(nodes);
            return nodes;
        }
    }

    public static class Edge {
        public long source, target;
        public String type;

        public Edge(){}

        public Edge(GrRelation base) {
            source = base.getStartNode().getId();
            target = base.getEndNode().getId();
            type = base.getType();
        }

        public static Edge[] fromList(List<GrRelation> matches) {
            HashSet found = new HashSet<Edge>(matches.size());
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) != null)
                    found.add(new Edge(matches.get(i)));
            }
            Edge[] arrows = new Edge[found.size()];
            found.toArray(arrows);
            return arrows;
        }

        @Override
        public int hashCode(){
            return (source+type+target).hashCode();
        }
    }
}