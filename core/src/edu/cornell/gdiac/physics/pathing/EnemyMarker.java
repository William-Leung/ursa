package edu.cornell.gdiac.physics.pathing;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;

public class EnemyMarker {

	private final Vector2 pos;
	private float[] rotations = null;
	private float rotationSpeed = 5;
	private int rotationDelay = 60;

	public EnemyMarker(Vector2 p, JsonValue properties) {
		pos = p;

		// Parse all custom properties for this marker
		if (properties != null) {
			for(int i = 0; i < properties.size; i++) {
				JsonValue property = properties.get(i);

				switch (property.get("name").asString()) {
					case "rotations":
						String[] split = property.get("value").asString().split(",");
						rotations = new float[split.length];

						for (int j = 0; j < split.length; j++) {
							rotations[j] = Float.parseFloat(split[j]);
						}
						break;

					case "rotation_speed":
						rotationSpeed = property.get("value").asFloat();
						break;

					case "rotation_delay":
						rotationDelay = property.get("value").asInt();
						break;
				}
			}
		}
	}

	public Vector2 getPosition() {
		return pos;
	}

	public float[] getRotations() {
		return rotations;
	}

	public float getRotationSpeed() {
		return rotationSpeed;
	}

	public int getRotationDelay() {
		return rotationDelay;
	}

}
