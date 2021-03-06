package com.github.bluenote

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL11



trait VertexData {
  
  val rawData: Array[Float]
  val primitiveType: Int
  
  val floatsPerVertex: Int
  val numVertices: Int
  val strideInBytes: Int
  
  def setVertexAttribArrayAndPointer(shader: Shader)
  
  def ++(that: VertexData): VertexData
}



class VertexData3D_NC(val rawData: Array[Float], val primitiveType: Int = GL11.GL_TRIANGLES) extends VertexData {

  val floatsPerVertex = 10
  val numVertices = rawData.length / floatsPerVertex
  val strideInBytes = floatsPerVertex * 4
  
  assert(rawData.length % floatsPerVertex == 0)
  
  def ++(that: VertexData): VertexData3D_NC = new VertexData3D_NC(this.rawData ++ that.rawData)
  
  def setVertexAttribArrayAndPointer(shader: Shader) {
    shader.vertexAttributes match {
      case va: VertexAttributes with HasVrtxAttrPos3D with HasVrtxAttrNormal with HasVrtxAttrColor =>
        GL20.glEnableVertexAttribArray(va.locPos3D)
        GL20.glEnableVertexAttribArray(va.locNormal)
        GL20.glEnableVertexAttribArray(va.locColor)
        GlWrapper.checkGlError("after enabling vertex attrib array")
        GL20.glVertexAttribPointer(va.locPos3D,  3, GL11.GL_FLOAT, false, strideInBytes, 0)
        GL20.glVertexAttribPointer(va.locNormal, 3, GL11.GL_FLOAT, false, strideInBytes, 12)
        GL20.glVertexAttribPointer(va.locColor,  4, GL11.GL_FLOAT, false, strideInBytes, 24)
        GlWrapper.checkGlError("after setting the attrib pointers")
      case _ => throw new Exception("Shader does not provide required vertex attributes")
    }
  }
  
  /**
   * Generic modification of an existing VNC data according to a transformation matrix
   * 
   * In general transforming VNC data requires to transform both position and normals.
   * In case of non-uniform scaling the transformation of positions and normals is not
   * the same, therefore the most general form requires two transformation matrices, 
   * which is obviously annoying. See below for alternatives.
   */
  def transform(modMatrixPos: Mat4f, modMatrixNrm: Mat3f): VertexData3D_NC = {

    val newVertexData = rawData.clone()
    
    for (ii <- Range(0, numVertices)) {
      val i = ii*floatsPerVertex
      val pos = new Vec4f(rawData(i)  , rawData(i+1), rawData(i+2), 1f)
      val nrm = new Vec3f(rawData(i+3), rawData(i+4), rawData(i+5))
      
      val newPos = modMatrixPos * pos
      val newNrm = modMatrixNrm * nrm
      
      newVertexData(i  ) = newPos.x
      newVertexData(i+1) = newPos.y
      newVertexData(i+2) = newPos.z
      newVertexData(i+3) = newNrm.x
      newVertexData(i+4) = newNrm.y
      newVertexData(i+5) = newNrm.z
    }
    
    return new VertexData3D_NC(newVertexData, primitiveType)
  }
  
  /**
   * This provides a simplified interface of the above transformation.
   * The normal transformation matrix is calculated internally by taking the inverse transpose.
   */
  def transform(modMatrixPos: Mat4f): VertexData3D_NC = {
    val modMatrixNrm = Mat3f.createFromMat4f(modMatrixPos).inverse().transpose()
    transform(modMatrixPos, modMatrixNrm)
  }

  /**
   * In case our transformations have uniform scale the above is overkill;
   * we can simply use the position transformation matrix for normals as well
   */
  def transformSimple(modMatrixPos: Mat4f): VertexData3D_NC = {
    transform(modMatrixPos, Mat3f.createFromMat4f(modMatrixPos))
  }  
}






/**
 * Collection of vertex data generators for a few basic shapes.
 * Return VertexData is of type VertexData3D_NC
 */
object VertexDataGen3D_NC {
  
  // some shorthands to simplify notation
  type V = Vec3f
  case class C(r: Float, g: Float, b: Float, a: Float) {
    def arr = Array(r, g, b, a)
  }
  
  
  /**
   * Generic cube
   */  
  def cube(x1: Float, x2: Float, y1: Float, y2: Float, z1: Float, z2: Float, color: Color): VertexData3D_NC = {
    val p1 = Vec3f(x1 min x2, y1 min y2, z1 max z2)
    val p2 = Vec3f(x1 max x2, y1 min y2, z1 max z2)
    val p3 = Vec3f(x1 min x2, y1 max y2, z1 max z2)
    val p4 = Vec3f(x1 max x2, y1 max y2, z1 max z2)
    val p5 = Vec3f(x1 min x2, y1 min y2, z1 min z2)
    val p6 = Vec3f(x1 max x2, y1 min y2, z1 min z2)
    val p7 = Vec3f(x1 min x2, y1 max y2, z1 min z2)
    val p8 = Vec3f(x1 max x2, y1 max y2, z1 min z2)
    val carr = color.toArr
    val triangles =
      // front face
      p1.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p2.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p4.arr ++ Vec3f(0,0,+1).arr ++ carr ++
      p4.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p3.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p1.arr ++ Vec3f(0,0,+1).arr ++ carr ++
      // back face
      p5.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p7.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p8.arr ++ Vec3f(0,0,-1).arr ++ carr ++
      p8.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p6.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p5.arr ++ Vec3f(0,0,-1).arr ++ carr ++
      // right face
      p2.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p6.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p8.arr ++ Vec3f(+1,0,0).arr ++ carr ++
      p8.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p4.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p2.arr ++ Vec3f(+1,0,0).arr ++ carr ++
      // left face
      p1.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p3.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p7.arr ++ Vec3f(-1,0,0).arr ++ carr ++
      p7.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p5.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p1.arr ++ Vec3f(-1,0,0).arr ++ carr ++
      // top face
      p3.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p4.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p8.arr ++ Vec3f(0,+1,0).arr ++ carr ++
      p8.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p7.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p3.arr ++ Vec3f(0,+1,0).arr ++ carr ++
      // bottom face
      p1.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p5.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p6.arr ++ Vec3f(0,-1,0).arr ++ carr ++
      p6.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p2.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p1.arr ++ Vec3f(0,-1,0).arr ++ carr
    return new VertexData3D_NC(triangles)
  }
  
  /**
   * Generic cylinder
   * Convention:
   * x/z   corresponds to     rotation plane,
   * y     corresponds to     cylinder axis (with top a +h and bottom at -h)
   * 
   */
  def cylinder(r: Float, h: Float, color: Color, slices: Int = 4, wallOnly: Boolean = false): VertexData3D_NC = {
    val carr = color.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate wall:
    val wallTriangles = circularSlidingTuples.flatMap{ case (i,j) =>
      val p1 = Vec3f(sinValues(i), -h, cosValues(i))
      val p2 = Vec3f(sinValues(j), -h, cosValues(j))
      val p3 = Vec3f(sinValues(j), +h, cosValues(j))
      val p4 = Vec3f(sinValues(i), +h, cosValues(i))
      val normalI = Vec3f(sinValues(i)/r, 0, cosValues(i)/r)
      val normalJ = Vec3f(sinValues(j)/r, 0, cosValues(j)/r)
      p1.arr ++ normalI.arr ++ carr    ++    p2.arr ++ normalJ.arr ++ carr    ++    p3.arr ++ normalJ.arr ++ carr ++
      p3.arr ++ normalJ.arr ++ carr    ++    p4.arr ++ normalI.arr ++ carr    ++    p1.arr ++ normalI.arr ++ carr
    }
    if (wallOnly) {
      return new VertexData3D_NC(wallTriangles)
    }
    
    // generate planes:
    val planes = for ((y,n) <- List((-h, Vec3f(0,-1,0)), (+h, Vec3f(0,+1,0)))) yield {
      val pc = Vec3f(0, y, 0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val (ii, jj) = if (y > 0) (i,j) else (j,i) // change order depending on side
        val p1 = Vec3f(sinValues(ii), y, cosValues(ii))
        val p2 = Vec3f(sinValues(jj), y, cosValues(jj))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }

    return new VertexData3D_NC(wallTriangles ++ planes(0) ++ planes(1))
  }
  def cylinderTwoColors(r: Float, h: Float, colorBottom: Color, colorTop: Color, slices: Int = 4, wallOnly: Boolean = false): VertexData3D_NC = {
    val carrB = colorBottom.toArr
    val carrT = colorTop.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate wall:
    val wallTriangles = circularSlidingTuples.flatMap{ case (i,j) =>
      val p1 = Vec3f(sinValues(i), -h, cosValues(i))
      val p2 = Vec3f(sinValues(j), -h, cosValues(j))
      val p3 = Vec3f(sinValues(j), +h, cosValues(j))
      val p4 = Vec3f(sinValues(i), +h, cosValues(i))
      val normalI = Vec3f(sinValues(i)/r, 0, cosValues(i)/r)
      val normalJ = Vec3f(sinValues(j)/r, 0, cosValues(j)/r)
      p1.arr ++ normalI.arr ++ carrB    ++    p2.arr ++ normalJ.arr ++ carrB    ++    p3.arr ++ normalJ.arr ++ carrT ++
      p3.arr ++ normalJ.arr ++ carrT    ++    p4.arr ++ normalI.arr ++ carrT    ++    p1.arr ++ normalI.arr ++ carrB
    }
    if (wallOnly) {
      return new VertexData3D_NC(wallTriangles)
    }
    
    // generate planes:
    val planes = for ((y,n, carr) <- List((-h, Vec3f(0,-1,0), carrB), (+h, Vec3f(0,+1,0), carrT))) yield {
      val pc = Vec3f(0, y, 0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val (ii, jj) = if (y > 0) (i,j) else (j,i) // change order depending on side
        val p1 = Vec3f(sinValues(ii), y, cosValues(ii))
        val p2 = Vec3f(sinValues(jj), y, cosValues(jj))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }

    return new VertexData3D_NC(wallTriangles ++ planes(0) ++ planes(1))
  }
  
  /**
   * A "line" is a thin cylinder connecting two arbitrary points in space
   */
  def line(r: Float, p1: Vec3f, p2: Vec3f, color1: Color, color2: Color, slices: Int = 4, wallOnly: Boolean = false): VertexData3D_NC = {
    
    val p1_to_p2 = p2 - p1
    val p1_to_p2_norm = p1_to_p2.normalize
    
    val mid = p1 mid p2
    val halfLength = p1_to_p2.length / 2
    
    val cylinder = cylinderTwoColors(r, halfLength, color1, color2, slices, wallOnly)
    
    val cylNorm = Vec3f(0, 1, 0)
    val rotAxis = p1_to_p2_norm cross cylNorm
    val rotAngl = math.acos(p1_to_p2_norm   *   cylNorm).toFloat
    
    //println(rotAngl, rotAngl*180/math.Pi.toFloat, rotAxis)
    
    return cylinder.transform(Mat4f.translate(mid.x, mid.y, mid.z).rotate(-rotAngl*180/math.Pi.toFloat, rotAxis.x, rotAxis.y, rotAxis.z))
  }
  
  /**
   * Generic disk
   * Convention: centered at y=0, with normal in +y direction
   */
  def discVNC(r: Float, color: Color, slices: Int = 16): VertexData3D_NC = {
    val carr = color.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate planes:
    val disc = {
      val pc = Vec3f(0,0,0)
      val n  = Vec3f(0,1,0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val p1 = Vec3f(sinValues(i), 0, cosValues(i))
        val p2 = Vec3f(sinValues(j), 0, cosValues(j))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }
    return new VertexData3D_NC(disc)
  }  
  
  /**
   * Generic sphere
   */
  def sphere(r: Float, color: Color, numRecursions: Int = 4): VertexData3D_NC = {
    val carr = color.toArr

    val p1 = Vec3f(0, -r, 0)
    val p2 = Vec3f(0, 0, +r)
    val p3 = Vec3f(+r, 0, 0)
    val p4 = Vec3f(0, 0, -r)
    val p5 = Vec3f(0, +r, 0)
    val p6 = Vec3f(-r, 0, 0)
    
    val triangles = 
      (p1, p3, p2) ::
      (p1, p4, p3) ::
      (p1, p6, p4) ::
      (p1, p2, p6) ::
      (p5, p2, p3) ::
      (p5, p3, p4) ::
      (p5, p4, p6) ::
      (p5, p6, p2) ::
      Nil

    def midPoint(p1: Vec3f, p2: Vec3f) = (p1 mid p2).setLengthTo(r)
      
    def recursiveRefinement(triangles: List[(Vec3f, Vec3f, Vec3f)], numRecursions: Int): List[(Vec3f, Vec3f, Vec3f)] = {
      if (numRecursions==0) {
        return triangles 
      } else {
        val refinedTriangles = triangles.flatMap{ case (p1, p2, p3) =>
          val p4 = midPoint(p1, p2)
          val p5 = midPoint(p2, p3)
          val p6 = midPoint(p3, p1)
          (p1, p4, p6) ::
          (p4, p2, p5) ::
          (p4, p5, p6) ::
          (p6, p5, p3) ::
          Nil          
        }
        return recursiveRefinement(refinedTriangles, numRecursions-1)
      }
    }
      
    val refinedTriangles = recursiveRefinement(triangles, numRecursions)
    
    def vecToNormal(p: Vec3f) = p / r

    val allTriangles = refinedTriangles.toArray.flatMap{vertices => 
      vertices._1.arr ++ vecToNormal(vertices._1).arr ++ carr ++
      vertices._2.arr ++ vecToNormal(vertices._2).arr ++ carr ++
      vertices._3.arr ++ vecToNormal(vertices._3).arr ++ carr
    }
    
    return new VertexData3D_NC(allTriangles)
  }
  

  
  /**
   * Rounded cube 
   * Simplified over the generic cube in the sense that it is always centered at 0,
   * i.e., size is specified in "half width"
   */  
  /*
  def roundedCubeVNC(hwx: Float, hwy: Float, hwz: Float, r: Float, color: Color, detail: Int = 4): VertexData3D_NC = {
    val p1 = Vec3f(-hwx, -hwy, +hwz)
    val p2 = Vec3f(+hwx, -hwy, +hwz)
    val p3 = Vec3f(-hwx, +hwy, +hwz)
    val p4 = Vec3f(+hwx, +hwy, +hwz)
    val p5 = Vec3f(-hwx, -hwy, -hwz)
    val p6 = Vec3f(+hwx, -hwy, -hwz)
    val p7 = Vec3f(-hwx, +hwy, -hwz)
    val p8 = Vec3f(+hwx, +hwy, -hwz)
    val carr = color.toArr
    val triangles =
      // front face
      (p1+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p2+Vec3f(-r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p4+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr ++
      (p4+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p3+Vec3f(+r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p1+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr ++
      // back face
      (p5+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p7+Vec3f(+r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p8+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr ++
      (p8+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p6+Vec3f(-r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p5+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr ++
      // right face
      (p2+Vec3f( 0,+r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p6+Vec3f( 0,+r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p8+Vec3f( 0,-r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr ++
      (p8+Vec3f( 0,-r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p4+Vec3f( 0,-r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p2+Vec3f( 0,+r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr ++
      // left face
      (p1+Vec3f( 0,+r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p3+Vec3f( 0,-r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p7+Vec3f( 0,-r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr ++
      (p7+Vec3f( 0,-r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p5+Vec3f( 0,+r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p1+Vec3f( 0,+r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr ++
      // top face
      (p3+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p4+Vec3f(-r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p8+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr ++
      (p8+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p7+Vec3f(+r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p3+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr ++
      // bottom face
      (p1+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p5+Vec3f(+r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p6+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr ++
      (p6+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p2+Vec3f(-r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p1+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr
    
    

    val hwxr = hwx-r
    val hwyr = hwy-r
    val hwzr = hwz-r

    val cylinderY = cylinderVNC(r, hwyr, color, detail*4, true).rawData
    val lengthOfBlock = cylinderY.length / 4

    val cylinderYp2p4 = Array.tabulate(lengthOfBlock)(i => cylinderY(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, 0, +hwzr))
    val cylinderYp6p8 = Array.tabulate(lengthOfBlock)(i => cylinderY(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, 0, -hwzr))
    val cylinderYp5p7 = Array.tabulate(lengthOfBlock)(i => cylinderY(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, 0, -hwzr))
    val cylinderYp1p3 = Array.tabulate(lengthOfBlock)(i => cylinderY(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, 0, +hwzr))

    val cylinderX = cylinderVNC(r, hwxr, color, detail*4, true).transfromSimpleVNC(Mat4f.rotate(-90, 0, 0, 1))
    
    val cylinderXp1p2 = Array.tabulate(lengthOfBlock)(i => cylinderX(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, -hwyr, +hwzr))
    val cylinderXp5p6 = Array.tabulate(lengthOfBlock)(i => cylinderX(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, -hwyr, -hwzr))
    val cylinderXp7p8 = Array.tabulate(lengthOfBlock)(i => cylinderX(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, +hwyr, -hwzr))
    val cylinderXp3p4 = Array.tabulate(lengthOfBlock)(i => cylinderX(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, +hwyr, +hwzr))

    val cylinderZ = cylinderVNC(r, hwzr, color, detail*4, true).transfromSimpleVNC(Mat4f.rotate(90, 1, 0, 0))
    
    val cylinderZp2p6 = Array.tabulate(lengthOfBlock)(i => cylinderZ(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, 0))
    val cylinderZp4p8 = Array.tabulate(lengthOfBlock)(i => cylinderZ(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, 0))
    val cylinderZp3p7 = Array.tabulate(lengthOfBlock)(i => cylinderZ(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, 0))
    val cylinderZp1p5 = Array.tabulate(lengthOfBlock)(i => cylinderZ(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, 0))

    // the following ensures that the sphere uses just the right number of triangles
    // in order to math the number of faces used for the cylinder (at least for power-of-2 detail values)
    // otherwise there are visible gaps between the cylinder and the sphere parts
    val sphereDetail = (math.log(detail) / math.log(2)).toInt
    val sphere = sphereVNC(r, color, sphereDetail)
    val lengthOneEighth = sphere.length / 8
    
    val sphere1 = Array.tabulate(lengthOneEighth)(i => sphere(0*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, +hwzr))
    val sphere2 = Array.tabulate(lengthOneEighth)(i => sphere(1*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, -hwzr))
    val sphere3 = Array.tabulate(lengthOneEighth)(i => sphere(2*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, -hwzr))
    val sphere4 = Array.tabulate(lengthOneEighth)(i => sphere(3*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, +hwzr))
    val sphere5 = Array.tabulate(lengthOneEighth)(i => sphere(4*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, +hwzr))
    val sphere6 = Array.tabulate(lengthOneEighth)(i => sphere(5*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, -hwzr))
    val sphere7 = Array.tabulate(lengthOneEighth)(i => sphere(6*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, -hwzr))
    val sphere8 = Array.tabulate(lengthOneEighth)(i => sphere(7*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, +hwzr))
    
    return triangles ++ cylinderYp2p4 ++ cylinderYp6p8 ++ cylinderYp5p7 ++ cylinderYp1p3 ++
                        cylinderXp1p2 ++ cylinderXp5p6 ++ cylinderXp7p8 ++ cylinderXp3p4 ++
                        cylinderZp2p6 ++ cylinderZp4p8 ++ cylinderZp3p7 ++ cylinderZp1p5 ++
                        sphere1 ++ sphere2 ++ sphere3 ++ sphere4 ++ sphere5 ++ sphere6 ++ sphere7 ++ sphere8
  }

  */
  
}











