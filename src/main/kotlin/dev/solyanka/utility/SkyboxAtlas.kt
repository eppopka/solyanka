/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Geometry for one shared outer image and eight exact replacement crops in a padded atlas. */
object SkyboxAtlas {
  const val FRAME_SIZE = 1024
  const val X0 = 109
  const val Y0 = 339
  const val X1 = 910
  const val Y1 = 659
  const val CELL_WIDTH = 803
  const val CELL_HEIGHT = 322
  const val WIDTH = 1606
  const val HEIGHT = 1288
  const val BASE_FIRST_INDEX = 36
  const val FRAME_FIRST_INDEX = 60
  const val INDEX_COUNT = 108

  fun frameIndex(frame: Int): Int {
    require(frame in 0..7)
    return FRAME_FIRST_INDEX + frame * 6
  }

  /** Six cube faces, four non-overlapping base strips, then the eight atlas frame quads. */
  fun vertices(): List<SkyVertex> {
    val source = SkyboxLogic.vertices()
    val quad = source.takeLast(4)
    val origin = quad[0].position
    val dx = quad[1].position + origin * (-1.0)
    val dy = quad[3].position + origin * (-1.0)
    fun point(u: Double, v: Double) = origin + dx * u + dy * v
    fun rect(
      x0: Int,
      y0: Int,
      x1: Int,
      y1: Int,
      tu0: Float,
      tv0: Float,
      tu1: Float,
      tv1: Float,
    ): List<SkyVertex> {
      val l = x0.toDouble() / FRAME_SIZE
      val r = x1.toDouble() / FRAME_SIZE
      val t = y0.toDouble() / FRAME_SIZE
      val b = y1.toDouble() / FRAME_SIZE
      return listOf(
        SkyVertex(point(l, t), tu0, tv0),
        SkyVertex(point(r, t), tu1, tv0),
        SkyVertex(point(r, b), tu1, tv1),
        SkyVertex(point(l, b), tu0, tv1),
      )
    }
    fun base(x0: Int, y0: Int, x1: Int, y1: Int) =
      rect(
        x0,
        y0,
        x1,
        y1,
        x0.toFloat() / FRAME_SIZE,
        y0.toFloat() / FRAME_SIZE,
        x1.toFloat() / FRAME_SIZE,
        y1.toFloat() / FRAME_SIZE,
      )
    return buildList {
      addAll(source.take(24))
      addAll(base(0, 0, FRAME_SIZE, Y0))
      addAll(base(0, Y1, FRAME_SIZE, FRAME_SIZE))
      addAll(base(0, Y0, X0, Y1))
      addAll(base(X1, Y0, FRAME_SIZE, Y1))
      for (i in 0..7) {
        val x = (i % 2) * CELL_WIDTH + 1
        val y = (i / 2) * CELL_HEIGHT + 1
        addAll(
          rect(
            X0,
            Y0,
            X1,
            Y1,
            x.toFloat() / WIDTH,
            y.toFloat() / HEIGHT,
            (x + X1 - X0).toFloat() / WIDTH,
            (y + Y1 - Y0).toFloat() / HEIGHT,
          ),
        )
      }
    }
  }
}
