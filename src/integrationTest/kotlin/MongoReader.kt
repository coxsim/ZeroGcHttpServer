import org.litote.kmongo.* //NEEDED! import KMongo extensions


data class Jedi(val name: String, val age: Int)

class MongoReader {
    private val connectionString = "mongodb://scoxadmin:admin123@docdb-2020-12-23-08-25-08.cluster-cfuer3nxdnbj.us-east-2.docdb.amazonaws.com:27017/?ssl=true&ssl_ca_certs=rds-combined-ca-bundle.pem&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false"

    private val client = KMongo.createClient(connectionString) //get com.mongodb.MongoClient new instance
    private val database = client.getDatabase("test") //normal java driver usage
    private val col = database.getCollection<Jedi>() //KMongo extension method
//here the name of the collection by convention is "jedi"
//you can use getCollection<Jedi>("otherjedi") if the collection name is different

    init {
        col.insertOne(Jedi("Luke Skywalker", 19))
    }

    val yoda : Jedi? = col.findOne(Jedi::name eq "Yoda")
}

fun main() {
    MongoReader()
}