package com.github.fernthedev.secret_santa

import com.github.fernthedev.config.common.Config
import com.github.fernthedev.config.gson.GsonConfig
import com.github.fernthedev.fernutils.console.ArgumentArrayUtils
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random
import kotlin.system.exitProcess

val config: Config<ConfigData> = GsonConfig(ConfigData(), File("./config.json"))

fun main(args: Array<String>) {
    val debug = args.contains("-debug")
    val console = System.console()

    if (console == null && !debug) {
        JOptionPane.showMessageDialog(JFrame(), "Please open in a console", "Error", JOptionPane.ERROR_MESSAGE)
        exitProcess(-1)
    }

    config.load()
    config.save()

    var file = File("list.txt")
    var seed: Long? = null

    ArgumentArrayUtils.parseArguments(args,
        "--listfile" to {queue -> file = File(queue.remove()) },
                "--seed" to {queue -> seed = queue.remove().toLong()}
    )

    if (seed == null) {
        seed = Random.Default.nextLong()
    }

    if (!file.exists()) {
        if (!file.createNewFile()) throw FileSystemException(file, reason = "Unable to create file")

        error("Please fill ${file.canonicalPath} with list of people")
    }

    val list = getList(file)

    if (config.configData.reproduceOrder) list.sort()

    requireNotNull(seed)
    val map = getSecretSanta(list, Random(seed!!))

    var folder: File? = null

    println("Where do you want this to be saved? (Must be a folder)")

    while (folder == null) {

        val input = console.readLine()

        if (!File(input).exists()) {
            System.err.println("Cannot find folder $input")
            continue
        }

        folder = File(input)

    }


    map.forEach { (secretSanta, receiver) ->

        createSecretSantaFile(folder, secretSanta, receiver)

    }

    val seedFile = File(folder, "seed.txt")

    if (!seedFile.exists())
        seedFile.createNewFile()

    seedFile.sink().buffer().use { sink ->
        sink.writeUtf8(seed.toString())
    }

}

fun getSecretSanta(list: List<String>, random: Random): Map<String, String> {

    if (list.isEmpty())
        error("The list is empty. No secret Santas? ;-;")

    if (list.size <= 2)
        error("You need at least 3 or more people in the list")


    // Santa -> Person
    // The key is the secret santa of the value who is a person.
    val map = HashMap<String, String>()


    list.forEach {santa ->

        // Find any random person on the list
        var candidates = list.stream()
            .filter { candidate -> candidate != santa && !map.values.contains(candidate) } // Remove people with a secret santa or the person itself
            .collect(Collectors.toList())

        // Swap someone with the last person on the list
        // Best solution I could find
        if (candidates.isEmpty()) {
            // Swap the person with someone else
            val oldSanta: String = map.keys.elementAt(random.nextInt(map.size - 1))
            val oldCandidate: String = map[oldSanta]!!

            map[santa] = oldCandidate
            map[oldSanta] = santa
            return@forEach
        }

        // If there's more than 2 people, avoid making them be their own secret santa
        if (candidates.size > 1) candidates = candidates.filter { candidate -> map[candidate] != santa }

        val index = if (candidates.size == 1) 0 else random.nextInt(candidates.size - 1)

        map[santa] = candidates[index]

    }

    validateList(list, map)

    return map
}

fun validateList(list: List<String>, map: Map<String, String>) {
    list.forEach { s ->
        require(map.containsKey(s) && map.containsValue(s)) {"The map does not contain $s in either key or value. $map"}
    }
}

@Throws(IOException::class)
fun createSecretSantaFile(folder: File, secretSanta: String, receiver: String) {
    val santaFile = File(folder, "${secretSanta}.txt")

    if (!santaFile.exists())
        santaFile.createNewFile()

    santaFile.sink().buffer().use { sink ->
        sink.writeUtf8("You are ${receiver}'s Secret Santa! Give them some great gifts and hugs! :D")
    }
}


@Throws(IOException::class)
fun getList(file: File): ArrayList<String> {
    val list = ArrayList<String>()

    file.source().use { fileSource ->
        fileSource.buffer().use { bufferedFileSource ->
            while (true) {
                val line = bufferedFileSource.readUtf8Line() ?: break

                if (line.trim().isEmpty()) continue

                list.add(line.trim())
            }
        }
    }

    return list
}

