package cc.mcyx.paimon

import cc.mcyx.paimon.common.PaimonPlugin
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate
import org.sonatype.aether.RepositorySystemSession
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.repository.RemoteRepository
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.jar.JarFile

/**
 * 打包类
 * @author zcc
 */
@Mojo(name = "paimonPack")
class PaimonMavenPlugin : AbstractMojo() {
    @Parameter(name = "name", defaultValue = "no set")
    lateinit var name: String

    @Parameter(name = "main", defaultValue = "no set")
    lateinit var main: String

    @Parameter(name = "version", defaultValue = "no set")
    lateinit var version: String

    @Parameter(name = "description", defaultValue = "no set")
    lateinit var description: String

    @Parameter(name = "authors", defaultValue = "no set")
    lateinit var authors: MutableList<String>

    //打包目录
    private val buildDir = File("target")

    //依赖目录
    private val libDir = File(buildDir, "lib")

    //类目录
    private val classes = File(buildDir, "classes")

    @Parameter(defaultValue = "\${project}")
    lateinit var project: MavenProject

    //Paimon 依赖版本
    private lateinit var paimon: Dependency

    override fun execute() {
        log.info("Paimon Packing....")
        this.buildDependency()
        writePluginYaml()
    }

    /**
     * 打包 Paimon 依赖与项目融合
     */
    private fun buildDependency() {
        project.dependencies.forEach { dependency ->
            //打包Paimon依赖
            if (dependency.groupId == "cc.mcyx" && dependency.artifactId == "Paimon") {
                this.paimon = dependency
                val file = File(libDir, "${dependency.artifactId}-${dependency.version}.jar")
                log.info("out lib... ${file.name}")
                val jarFile = JarFile(file)
                for (entry in jarFile.entries()) {
                    File(classes, entry.name).also {
                        if (entry.isDirectory) {
                            it.mkdirs()
                        } else {
                            it.createNewFile()
                            //写出文件
                            it.writeBytes(jarFile.getInputStream(entry).readBytes())
                        }
                    }
                }
            }
        }

    }

    /**
     * 打包 Plugin.yml
     */
    private fun writePluginYaml() {
        val pluginYaml = File(classes, "plugin.yml")
        val pluginMap = linkedMapOf<String, Any>()
        pluginMap["name"] = name
        pluginMap["main"] = main
        pluginMap["version"] = version
        pluginMap["description"] = description
        pluginMap["authors"] = authors
        Yaml().also { yaml ->
            FileWriter(pluginYaml).also { fw ->
                run {
                    fw.write(
                        """
                            
                            # Paimon Framework build
                            # Paimon version ${paimon.version}
                            # Build ${Date()}
                            
                            
                        """.trimIndent()
                    )
                    yaml.dump(pluginMap, fw)
                    fw.close()
                }
            }
        }
    }
}