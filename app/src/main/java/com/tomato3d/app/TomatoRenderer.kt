package com.tomato3d.app

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class TomatoRenderer : GLSurfaceView.Renderer {

    var rotationX = -20f
    var rotationY = 30f

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(9)

    private var bodyProgram = 0
    private var stemProgram = 0
    private var leafProgram = 0

    private var bodyVao = 0
    private var bodyIndexCount = 0
    private var stemVao = 0
    private var stemIndexCount = 0
    private var leafVao = 0
    private var leafIndexCount = 0

    private var time = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.08f, 0.08f, 0.12f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        bodyProgram = createProgram(BODY_VERTEX_SHADER, BODY_FRAGMENT_SHADER)
        stemProgram = createProgram(STEM_VERTEX_SHADER, STEM_FRAGMENT_SHADER)
        leafProgram = createProgram(LEAF_VERTEX_SHADER, LEAF_FRAGMENT_SHADER)

        buildTomatoBody()
        buildStem()
        buildLeaf()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 35f, ratio, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 5.5f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        time += 0.016f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val mvpTranspose = invertAndTranspose4x4(mvpMatrix)
        extract3x3(mvpTranspose, normalMatrix)

        drawBody()
        drawStem()
        drawLeaves()
    }

    private fun drawBody() {
        GLES30.glUseProgram(bodyProgram)

        val mvpLoc = GLES30.glGetUniformLocation(bodyProgram, "uMVP")
        val modelLoc = GLES30.glGetUniformLocation(bodyProgram, "uModel")
        val normalLoc = GLES30.glGetUniformLocation(bodyProgram, "uNormalMatrix")
        val timeLoc = GLES30.glGetUniformLocation(bodyProgram, "uTime")
        val lightDirLoc = GLES30.glGetUniformLocation(bodyProgram, "uLightDir")
        val cameraLoc = GLES30.glGetUniformLocation(bodyProgram, "uCameraPos")

        GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(modelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix3fv(normalLoc, 1, false, normalMatrix, 0)
        GLES30.glUniform1f(timeLoc, time)
        GLES30.glUniform3f(lightDirLoc, 0.6f, 0.9f, 0.8f)
        GLES30.glUniform3f(cameraLoc, 0f, 0f, 5.5f)

        GLES30.glBindVertexArray(bodyVao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, bodyIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawStem() {
        GLES30.glUseProgram(stemProgram)

        val mvpLoc = GLES30.glGetUniformLocation(stemProgram, "uMVP")
        val modelLoc = GLES30.glGetUniformLocation(stemProgram, "uModel")
        val normalLoc = GLES30.glGetUniformLocation(stemProgram, "uNormalMatrix")
        val lightDirLoc = GLES30.glGetUniformLocation(stemProgram, "uLightDir")

        GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(modelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix3fv(normalLoc, 1, false, normalMatrix, 0)
        GLES30.glUniform3f(lightDirLoc, 0.6f, 0.9f, 0.8f)

        GLES30.glBindVertexArray(stemVao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, stemIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawLeaves() {
        GLES30.glUseProgram(leafProgram)

        val mvpLoc = GLES30.glGetUniformLocation(leafProgram, "uMVP")
        val modelLoc = GLES30.glGetUniformLocation(leafProgram, "uModel")
        val normalLoc = GLES30.glGetUniformLocation(leafProgram, "uNormalMatrix")
        val timeLoc = GLES30.glGetUniformLocation(leafProgram, "uTime")
        val lightDirLoc = GLES30.glGetUniformLocation(leafProgram, "uLightDir")

        GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(modelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix3fv(normalLoc, 1, false, normalMatrix, 0)
        GLES30.glUniform1f(timeLoc, time)
        GLES30.glUniform3f(lightDirLoc, 0.6f, 0.9f, 0.8f)

        GLES30.glBindVertexArray(leafVao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, leafIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun buildTomatoBody() {
        val latSteps = 64
        val lonSteps = 64
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..latSteps) {
            val lat = Math.PI * i / latSteps - Math.PI / 2.0
            for (j in 0..lonSteps) {
                val lon = 2.0 * Math.PI * j / lonSteps

                val ribCount = 8.0
                val ribAmount = 0.04 * sin(lat * 2) * cos(lon * ribCount)

                val squash = 0.82
                val r = (1.0 + ribAmount)

                var x = (r * cos(lat) * cos(lon)).toFloat()
                var y = (r * cos(lat) * sin(lon) * squash).toFloat()
                var z = (r * sin(lat)).toFloat()

                val topBias = ((sin(lat) + 1.0) / 2.0).pow(0.3)
                val creaseVal = (cos(lon * ribCount) * 0.5 + 0.5).pow(2.0)
                y *= (1f - creaseVal.toFloat() * 0.06f * topBias.toFloat())

                vertices.add(x); vertices.add(z); vertices.add(y)

                val nx = x; val nz = z; val ny = y
                val nl = sqrt(nx * nx + ny * ny + nz * nz)
                normals.add(nx / nl); normals.add(nz / nl); normals.add(ny / nl)
            }
        }

        for (i in 0 until latSteps) {
            for (j in 0 until lonSteps) {
                val a = (i * (lonSteps + 1) + j).toShort()
                val b = (a + (lonSteps + 1)).toShort()
                val c = (a + 1).toShort()
                val d = (b + 1).toShort()
                indices.add(a); indices.add(b); indices.add(c)
                indices.add(c); indices.add(b); indices.add(d)
            }
        }

        bodyIndexCount = indices.size
        bodyVao = createVao(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            indices.toShortArray()
        )
    }

    private fun buildStem() {
        val segments = 12
        val heightSteps = 4
        val radius = 0.04f
        val stemHeight = 0.25f
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..heightSteps) {
            val h = (i.toFloat() / heightSteps) * stemHeight
            val taper = 1f - (i.toFloat() / heightSteps) * 0.3f
            val r = radius * taper
            for (j in 0..segments) {
                val angle = 2f * PI.toFloat() * j / segments
                val x = r * cos(angle)
                val z = r * sin(angle)
                vertices.add(x)
                vertices.add(h + 1.05f)
                vertices.add(z)
                normals.add(cos(angle))
                normals.add(0f)
                normals.add(sin(angle))
            }
        }

        for (i in 0 until heightSteps) {
            for (j in 0 until segments) {
                val a = (i * (segments + 1) + j).toShort()
                val b = (a + (segments + 1)).toShort()
                val c = (a + 1).toShort()
                val d = (b + 1).toShort()
                indices.add(a); indices.add(b); indices.add(c)
                indices.add(c); indices.add(b); indices.add(d)
            }
        }

        stemIndexCount = indices.size
        stemVao = createVao(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            indices.toShortArray()
        )
    }

    private fun buildLeaf() {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val leafCount = 5

        for (leaf in 0 until leafCount) {
            val baseAngle = (2f * PI.toFloat() * leaf / leafCount)
            val vertBase = vertices.size / 3

            val tipX = 0.35f * cos(baseAngle)
            val tipZ = 0.35f * sin(baseAngle)

            val leftAngle = baseAngle + 0.3f
            val rightAngle = baseAngle - 0.3f
            val midAngle = baseAngle

            val p0 = floatArrayOf(0f, 1.08f, 0f)
            val p1 = floatArrayOf(0.12f * cos(leftAngle), 1.12f, 0.12f * sin(leftAngle))
            val p2 = floatArrayOf(tipX * 0.7f, 1.1f, tipZ * 0.7f)
            val p3 = floatArrayOf(tipX, 1.0f, tipZ)
            val p4 = floatArrayOf(tipX * 0.7f, 1.06f, tipZ * 0.7f)
            val p5 = floatArrayOf(0.12f * cos(rightAngle), 1.12f, 0.12f * sin(rightAngle))

            val up = floatArrayOf(0f, 1f, 0f)

            val pts = arrayOf(p0, p1, p2, p3, p4, p5)
            for (p in pts) {
                vertices.addAll(p.toList())
                normals.addAll(up.toList())
            }

            indices.add((vertBase + 0).toShort())
            indices.add((vertBase + 1).toShort())
            indices.add((vertBase + 2).toShort())
            indices.add((vertBase + 0).toShort())
            indices.add((vertBase + 2).toShort())
            indices.add((vertBase + 3).toShort())
            indices.add((vertBase + 0).toShort())
            indices.add((vertBase + 3).toShort())
            indices.add((vertBase + 4).toShort())
            indices.add((vertBase + 0).toShort())
            indices.add((vertBase + 4).toShort())
            indices.add((vertBase + 5).toShort())
        }

        leafIndexCount = indices.size
        leafVao = createVao(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            indices.toShortArray()
        )
    }

    private fun createVao(verts: FloatArray, norms: FloatArray, inds: ShortArray): Int {
        val vaoBuf = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoBuf, 0)
        val vao = vaoBuf[0]
        GLES30.glBindVertexArray(vao)

        val vboBuf = IntArray(2)
        GLES30.glGenBuffers(2, vboBuf, 0)

        val vertBuf = createFloatBuffer(verts)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboBuf[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        val normBuf = createFloatBuffer(norms)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboBuf[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, norms.size * 4, normBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(1)

        val eboBuf = createShortBuffer(inds)
        val ebo = IntArray(1)
        GLES30.glGenBuffers(1, ebo, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, inds.size * 2, eboBuf, GLES30.GL_STATIC_DRAW)

        GLES30.glBindVertexArray(0)
        return vao
    }

    companion object {
        private val BODY_VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform mat3 uNormalMatrix;
            out vec3 vNormal;
            out vec3 vWorldPos;
            void main() {
                vWorldPos = vec3(uModel * vec4(aPos, 1.0));
                vNormal = normalize(uNormalMatrix * aNormal);
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """.trimIndent()

        private val BODY_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec3 vNormal;
            in vec3 vWorldPos;
            uniform vec3 uLightDir;
            uniform vec3 uCameraPos;
            uniform float uTime;
            out vec4 fragColor;

            vec3 tomatoColor(vec3 n, vec3 pos) {
                float top = smoothstep(-0.3, 0.9, n.y);
                vec3 baseRed = vec3(0.85, 0.05, 0.03);
                vec3 deepRed = vec3(0.55, 0.02, 0.01);
                vec3 yellowGreen = vec3(0.65, 0.55, 0.15);
                vec3 color = mix(deepRed, baseRed, top);
                float sideLight = dot(normalize(vec3(n.x, 0.0, n.z)), normalize(vec3(uLightDir.x, 0.0, uLightDir.z)));
                color = mix(color, color * 1.15, max(0.0, sideLight) * 0.3);
                float subsurface = pow(max(0.0, dot(-normalize(uCameraPos - pos), uLightDir)), 4.0);
                color += vec3(0.9, 0.1, 0.02) * subsurface * 0.15;
                float noise = fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453);
                color += (noise - 0.5) * 0.02;
                float sheen = pow(max(0.0, dot(reflect(-uLightDir, n), normalize(uCameraPos - pos))), 64.0);
                color += vec3(1.0, 0.95, 0.9) * sheen * 0.6;
                float rim = 1.0 - max(0.0, dot(normalize(uCameraPos - pos), n));
                color += vec3(0.3, 0.05, 0.02) * pow(rim, 3.0) * 0.4;
                return color;
            }

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                vec3 V = normalize(uCameraPos - vWorldPos);
                float NdotL = max(dot(N, L), 0.0);
                float ambient = 0.25;
                float diffuse = NdotL * 0.65;
                float wrap = max(0.0, (NdotL + 0.3) / 1.3) * 0.15;
                vec3 color = tomatoColor(N, vWorldPos);
                float lighting = ambient + diffuse + wrap;
                vec3 finalColor = color * lighting;
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 80.0);
                finalColor += vec3(1.0, 0.95, 0.9) * spec * 0.35;
                finalColor = pow(finalColor, vec3(1.0 / 2.2));
                fragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        private val STEM_VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform mat3 uNormalMatrix;
            out vec3 vNormal;
            out vec3 vWorldPos;
            void main() {
                vWorldPos = vec3(uModel * vec4(aPos, 1.0));
                vNormal = normalize(uNormalMatrix * aNormal);
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """.trimIndent()

        private val STEM_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec3 vNormal;
            in vec3 vWorldPos;
            uniform vec3 uLightDir;
            out vec4 fragColor;
            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                float NdotL = max(dot(N, L), 0.0);
                vec3 stemColor = vec3(0.35, 0.25, 0.1);
                float lighting = 0.3 + NdotL * 0.6;
                vec3 finalColor = stemColor * lighting;
                finalColor = pow(finalColor, vec3(1.0 / 2.2));
                fragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        private val LEAF_VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform mat3 uNormalMatrix;
            out vec3 vNormal;
            out vec3 vWorldPos;
            void main() {
                vWorldPos = vec3(uModel * vec4(aPos, 1.0));
                vNormal = normalize(uNormalMatrix * aNormal);
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """.trimIndent()

        private val LEAF_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec3 vNormal;
            in vec3 vWorldPos;
            uniform vec3 uLightDir;
            uniform float uTime;
            out vec4 fragColor;
            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                float NdotL = max(dot(N, L), 0.0);
                vec3 leafColor = vec3(0.15, 0.45, 0.08);
                vec3 darkLeaf = vec3(0.08, 0.28, 0.04);
                float variation = sin(vWorldPos.x * 20.0 + vWorldPos.z * 15.0) * 0.5 + 0.5;
                vec3 color = mix(darkLeaf, leafColor, variation * 0.4 + 0.6);
                float lighting = 0.3 + NdotL * 0.6;
                vec3 finalColor = color * lighting;
                float backLight = max(0.0, dot(-N, L));
                finalColor += vec3(0.1, 0.3, 0.05) * backLight * 0.2;
                finalColor = pow(finalColor, vec3(1.0 / 2.2));
                fragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
            val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
            val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vs)
            GLES30.glAttachShader(program, fs)
            GLES30.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw RuntimeException("Program link failed: $log")
            }
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            return program
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Shader compile failed: $log")
            }
            return shader
        }

        private fun createFloatBuffer(data: FloatArray): FloatBuffer {
            val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(data).position(0)
            return fb
        }

        private fun createShortBuffer(data: ShortArray): ShortBuffer {
            val bb = ByteBuffer.allocateDirect(data.size * 2).order(ByteOrder.nativeOrder())
            val sb = bb.asShortBuffer()
            sb.put(data).position(0)
            return sb
        }

        private fun invertAndTranspose4x4(m: FloatArray): FloatArray {
            val out = FloatArray(16)
            val a00 = m[0]; val a01 = m[1]; val a02 = m[2]; val a03 = m[3]
            val a10 = m[4]; val a11 = m[5]; val a12 = m[6]; val a13 = m[7]
            val a20 = m[8]; val a21 = m[9]; val a22 = m[10]; val a23 = m[11]
            val a30 = m[12]; val a31 = m[13]; val a32 = m[14]; val a33 = m[15]

            val b00 = a00 * a11 - a01 * a10
            val b01 = a00 * a12 - a02 * a10
            val b02 = a00 * a13 - a03 * a10
            val b03 = a01 * a12 - a02 * a11
            val b04 = a01 * a13 - a03 * a11
            val b05 = a02 * a13 - a03 * a12
            val b06 = a20 * a31 - a21 * a30
            val b07 = a20 * a32 - a22 * a30
            val b08 = a20 * a33 - a23 * a30
            val b09 = a21 * a32 - a22 * a31
            val b10 = a21 * a33 - a23 * a31
            val b11 = a22 * a33 - a23 * a32

            var det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
            if (abs(det) < 1e-8f) return out
            det = 1.0f / det

            out[0] = (a11 * b11 - a12 * b10 + a13 * b09) * det
            out[1] = (a12 * b08 - a10 * b11 - a13 * b07) * det
            out[2] = (a10 * b10 - a11 * b08 + a13 * b06) * det
            out[3] = 0f
            out[4] = (a02 * b10 - a01 * b11 - a03 * b09) * det
            out[5] = (a00 * b11 - a02 * b08 + a03 * b07) * det
            out[6] = (a01 * b08 - a00 * b10 - a03 * b06) * det
            out[7] = 0f
            out[8] = (a31 * b05 - a32 * b04 + a33 * b03) * det
            out[9] = (a32 * b02 - a30 * b05 - a33 * b01) * det
            out[10] = (a30 * b04 - a31 * b02 + a33 * b00) * det
            out[11] = 0f
            out[12] = (a22 * b04 - a21 * b05 - a23 * b03) * det
            out[13] = (a20 * b05 - a22 * b02 + a23 * b01) * det
            out[14] = (a21 * b02 - a20 * b04 - a23 * b00) * det
            out[15] = 1f
            return out
        }

        private fun extract3x3(src: FloatArray, dst: FloatArray) {
            dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]
            dst[3] = src[4]; dst[4] = src[5]; dst[5] = src[6]
            dst[6] = src[8]; dst[7] = src[9]; dst[8] = src[10]
        }
    }
}
