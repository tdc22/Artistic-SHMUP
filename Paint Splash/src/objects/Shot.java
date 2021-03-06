package objects;

import math.VecMath;
import quaternion.Quaternionf;
import shader.Shader;
import vector.Vector3f;

public class Shot extends ShapedObject3 {
	RigidBody3 body, owner;
	Shader shotshader;

	public Shot(Vector3f spawnposition, ShapedObject3 shotgeometry, CollisionShape3 shotcollisionshape,
			Vector3f shotdirection, Shader shotshader, RigidBody3 owner) {
		super(spawnposition);
		this.shotshader = shotshader;
		this.owner = owner;
		setVAOHandle(shotgeometry.getVAOHandle());
		setVBOColorHandle(shotgeometry.getVBOColorHandle());
		setVBOIndexHandle(shotgeometry.getVBOIndexHandle());
		setVBOVertexHandle(shotgeometry.getVBOVertexHandle());
		setRenderedIndexCount(shotgeometry.getRenderedIndexCount());
		body = new RigidBody3(shotcollisionshape);
		body.setMass(1);
		body.setInertia(new Quaternionf());
		body.setLinearFactor(new Vector3f(1, 1, 1));
		body.applyCentralImpulse(VecMath.scale(shotdirection, -100));
	}

	public RigidBody3 getBody() {
		return body;
	}

	public RigidBody3 getOwner() {
		return owner;
	}

	public Shader getShotShader() {
		return shotshader;
	}
}