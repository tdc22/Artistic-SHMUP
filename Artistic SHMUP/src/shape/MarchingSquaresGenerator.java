package shape;

import objects.ShapedObject3;
import utils.GLConstants;
import vector.Vector3f;

public class MarchingSquaresGenerator {
	public static ShapedObject3 generate(boolean[][] grid, float gridwidth) {
		ShapedObject3 obj = new ShapedObject3();
		obj.setRenderMode(GLConstants.TRIANGLES);
		int index = 0;

		final float topY = 1;
		final float botY = -1;
		float halfgridwidth = gridwidth / 2f;

		for (int x = 0; x < grid.length - 1; x++) {
			for (int y = 0; y < grid[0].length - 1; y++) {
				boolean tl = grid[x][y];
				boolean tr = grid[x + 1][y];
				boolean bl = grid[x][y + 1];
				boolean br = grid[x + 1][y + 1];

				float tlX = x * gridwidth;
				float tlZ = y * gridwidth;
				float brX = tlX + gridwidth;
				float brZ = tlZ + gridwidth;

				if (!((tl && tr && bl && br) || (!tl && !tr && !bl && !br))) {
					if (tl && tr && bl) {
						System.out.println("1");
						obj.addVertex(new Vector3f(tlX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, brZ));

						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addTriangle(index, index + 4, index + 3);
						obj.addQuad(index + 2, index + 3, index + 6, index + 5);

						index += 7;
					} else if (tr && bl && br) {
						System.out.println("2");
						obj.addVertex(new Vector3f(brX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ));

						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addTriangle(index, index + 4, index + 3);
						obj.addQuad(index + 2, index + 3, index + 6, index + 5);

						index += 7;
					} else if (bl && br && tl) {
						System.out.println("3");
						obj.addVertex(new Vector3f(tlX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(brX, topY, brZ));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));
						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addTriangle(index, index + 4, index + 3);
						obj.addQuad(index + 2, index + 3, index + 6, index + 5);

						index += 7;
					} else if (br && tl && tr) {
						System.out.println("4");
						obj.addVertex(new Vector3f(brX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, brZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX, topY, tlZ));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));
						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addTriangle(index, index + 4, index + 3);
						obj.addQuad(index + 2, index + 3, index + 6, index + 5);

						index += 7;
					} else if (!tl && !tr && !bl) {
						System.out.println("5");
						obj.addVertex(new Vector3f(brX, topY, brZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));
						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addQuad(index + 1, index + 2, index + 4, index + 3);

						index += 5;
					} else if (!tr && !bl && !br) {
						System.out.println("6");
						obj.addVertex(new Vector3f(tlX, topY, tlZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));
						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addQuad(index + 1, index + 2, index + 4, index + 3);

						index += 5;
					} else if (!bl && !br && !tl) {
						System.out.println("7");
						obj.addVertex(new Vector3f(brX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));

						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addQuad(index + 1, index + 2, index + 4, index + 3);

						index += 5;
					} else if (!br && !tl && !tr) {
						System.out.println("8");
						obj.addVertex(new Vector3f(tlX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));

						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addQuad(index + 1, index + 2, index + 4, index + 3);

						index += 5;
					} else if (tl && tr) {
						System.out.println("9");
						obj.addVertex(new Vector3f(tlX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));

						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addQuad(index + 2, index + 3, index + 5, index + 4);

						index += 6;
					} else if (tr && br) {
						System.out.println("10");
						obj.addVertex(new Vector3f(brX, topY, tlZ));
						obj.addVertex(new Vector3f(brX, topY, brZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addQuad(index + 2, index + 3, index + 5, index + 4);

						index += 6;
					} else if (br && bl) {
						System.out.println("11");
						obj.addVertex(new Vector3f(brX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(brX, topY, tlZ + halfgridwidth));

						obj.addVertex(new Vector3f(tlX, botY, tlZ + halfgridwidth));
						obj.addVertex(new Vector3f(brX, botY, tlZ + halfgridwidth));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addQuad(index + 2, index + 3, index + 5, index + 4);

						index += 6;
					} else if (bl && tl) {
						System.out.println("12");
						obj.addVertex(new Vector3f(tlX, topY, brZ));
						obj.addVertex(new Vector3f(tlX, topY, tlZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, tlZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, topY, brZ));

						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, tlZ));
						obj.addVertex(new Vector3f(tlX + halfgridwidth, botY, brZ));

						obj.addTriangle(index, index + 2, index + 1);
						obj.addTriangle(index, index + 3, index + 2);
						obj.addQuad(index + 2, index + 3, index + 5, index + 4);

						index += 6;
					} else if (tl && br) {
						System.out.println("13");

					} else if (tr && bl) {
						System.out.println("14");

					}
				} else if (tl && tr && bl && br) {
					System.out.println("0");
					obj.addVertex(new Vector3f(tlX, topY, tlZ));
					obj.addVertex(new Vector3f(tlX, topY, brZ));
					obj.addVertex(new Vector3f(brX, topY, brZ));
					obj.addVertex(new Vector3f(brX, topY, tlZ));

					obj.addQuad(index, index + 1, index + 2, index + 3);
					index += 4;
				}
			}
		}
		obj.prerender();

		return obj;
	}
}
