using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using Mapbox.Unity.Map;
using System;
using Mapbox.Utils;
using Mapbox.Unity.Utilities;

public class AddMarkersToMap : MonoBehaviour {

	/// <summary>
	/// bla
	/// </summary>
	public AbstractMap Map;

	public TextAsset JSONFile;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Attraction;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Bbq;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Cafe;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject FastFood;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Mountain;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Museum;

	/// <summary>
	/// blub
	/// </summary>
	public GameObject Swim;

	private List<FeatureGameObject> GameObjects;

	// Use this for initialization
	void Start () {
		GameObjects = new List<FeatureGameObject> ();

		var myObject = JsonUtility.FromJson<JSONObject> (JSONFile.text);
		foreach (Feature f in myObject.features) {
			switch (f.properties.type) {
			case "cafe":
				this.CreateObject (f, Cafe);
				break;

			case "hiking":
				this.CreateObject (f, Mountain);
				break;

			case "climbing":
				this.CreateObject (f, Mountain);
				break;

			case "swim":
				this.CreateObject (f, Swim);
				break;

			case "bbq":
				this.CreateObject (f, Bbq);
				break;

			case "museum":
				this.CreateObject (f, Museum);
				break;

			case "view":
				this.CreateObject (f, Attraction);
				break;

			case "snack":
				this.CreateObject (f, FastFood);
				break;
			}
		}
	}

	void CreateObject(Feature f, GameObject go) {
		var myGO = Instantiate (go, Map.transform);
		myGO.SetActive (true);
		myGO.transform.Rotate(new Vector3(90f, 0, 0));

		myGO.AddComponent<BoxCollider> ();

		this.GameObjects.Add (new FeatureGameObject(f, myGO));
	}

	void Update() {
		foreach (FeatureGameObject fgo in GameObjects) {
			Vector2d position = new Vector2d (fgo.feature.geometry.coordinates[1], fgo.feature.geometry.coordinates[0]);
			Vector3 GeoWorldPosition = Map.GeoToWorldPosition (position, false);


			fgo.instance.transform.position = GeoWorldPosition;
			fgo.instance.transform.localScale = new Vector3 (1000f, 1000f, 1000f);
			fgo.instance.transform.localPosition += new Vector3 (0f, 10f + Map.QueryElevationInUnityUnitsAt (position), 0f);

			fgo.instance.transform.rotation = Quaternion.LookRotation(fgo.instance.transform.position - Camera.main.transform.position) * Quaternion.Euler(90, 0, 0);
		}

		if (Input.GetMouseButtonDown(0)) {
			RaycastHit hit;
			Ray ray = Camera.main.ScreenPointToRay(Input.mousePosition);

			if (Physics.Raycast (ray, out hit)) {
				Debug.Log (hit);
				if (hit.rigidbody != null) {
					Debug.Log (hit.transform.gameObject.name);
				}
			}

		}
	}

	public bool isOnMap(Vector3 position) {
		Debug.Log (position.x + " " + Map.transform.position.x + " " + Map.transform.localScale.x);
		return position.x < Map.transform.position.x + Map.transform.localScale.x &&
		position.x > Map.transform.position.x - Map.transform.localScale.x &&
		position.z < Map.transform.position.z + Map.transform.localScale.z &&
		position.z > Map.transform.position.z - Map.transform.localScale.z;
	}
}

public class FeatureGameObject {
	public Feature feature;
	public GameObject instance;

	public FeatureGameObject(Feature feature, GameObject instance) {
		this.feature = feature;
		this.instance = instance;
	}
}