[System.Serializable]
public class JSONObject {
	public Feature[] features; 
}

[System.Serializable]
public class Feature {
	public string type;
	public Property properties;
	public Geometry geometry;
	public string id;
}

[System.Serializable]
public class Property {
	public string type;
	public string name;
}

[System.Serializable]
public class Geometry {
	public float[] coordinates;
	public string type;
}

