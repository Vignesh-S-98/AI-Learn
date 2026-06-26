import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import java.util.*;
import java.lang.String;
import java.lang.Long;
import org.apache.log4j.Logger;
import groovy.io.FileType;
import com.jcraft.jsch.*;
import java.text.SimpleDateFormat;
import groovy.time.*;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import de.hybris.platform.servicelayer.impex.ImportConfig;
import de.hybris.platform.servicelayer.impex.ImportService;
import de.hybris.platform.servicelayer.impex.ImportResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;

this.CLEANUP_LOG_BUFFER = []

logger = Logger.getLogger("MediaUtil.class");
importService = spring.getBean('importService');
mediaService = spring.getBean('mediaService');
def startTime = new Date();




TRANSFER_PROTOCOL = "BLOB"; //SFTP OR BLOB OR LOCAL

azureSasToken = "sv=2024-11-04&ss=b&srt=sco&sp=rwdlacyx&se=2027-03-16T18:41:17Z&st=2026-03-16T10:26:17Z&spr=https&sig=cvrtSOWBsCTiQr5PnefrNKPi%2FYjCIP8YgmFaken0%2BQE%3D";
azureAccountName = "starxlhquathybcaas01";
azureContainerName = "dataload";
azureSourceDirectory = "media-extract-test/NLN/";



blobPath = azureSourceDirectory
localPath = ""



tenant = "nln"
page_num = "48"
file_num = '1'

pageNumPath = "page-"+page_num+"/"
file_name = "m_"+tenant+"_ProductMediasPkAndUrls_"+page_num+"-"+file_num+".impex"

blobFilePath = blobPath+pageNumPath +  file_name
localFilePath =  localPath+ pageNumPath +  file_name

IMPEX_IMPORT_THRESHOLD = 4000;
impexContent = "";
targetFileName = "";


if(TRANSFER_PROTOCOL == "BLOB")
{
    logMessage("prodMediaCleanupScript | Resolved Blob Path : " + blobFilePath, null)
    impexContent = readContentOfBlob(blobFilePath);
    targetFileName = blobFilePath;
    logMessage("prodMediaCleanupScript | Starting cleanup for "+blobFilePath);
}
if(TRANSFER_PROTOCOL == "LOCAL")
{
    logMessage("prodMediaCleanupScript | Resolved Local Path: " + localFilePath, null)
    impexContent = readLocalFileContent(localFilePath);
    targetFileName = org.apache.commons.io.FilenameUtils.getBaseName(localFilePath);
    logMessage("prodMediaCleanupScript | Starting cleanup for "+localFilePath);
}

logMessage("prodMediaCleanupScript | Impex content read successful.",null);
logLineSeparator();


importImpexBatchwise(impexContent);

def endTime = new Date();

// Calculate the duration
TimeDuration duration = TimeCategory.minus(endTime, startTime);

logMessage("prodMediaCleanupScript | Duration of media cleanup script execution for impex "+targetFileName+" : $duration",null);


def importImpexBatchwise(impexContent)
{
    logMessage("prodMediaCleanupScript | Initiating Import For Impex "+targetFileName,null);

    logMessage("prodMediaCleanupScript | impexContent length ",impexContent.length());

    logMessage("prodMediaCleanupScript | impexContentList size ",impexContent.readLines().size());

    def impexContentList = impexContent.readLines();

    String impexHeader = impexContentList.get(0);

    if(impexHeader.contains("REMOVE"))
    {
        logMessage("prodMediaCleanupScript | impexHeader "+impexHeader,null);

        impexContentList.removeAt(0);
    }
    else
    {
        impexHeader = "Remove Media;pk[unique=true];location\n";
    }

    def pagedContentLines = impexContentList.collate(IMPEX_IMPORT_THRESHOLD);

    def setCounter = 1;

    pagedContentLines.each { page ->

        logMessage("prodMediaCleanupScript | page size >> ",page.size());

        String impexSubSet = page.join('\n');


        impexSubSet = impexHeader + '\n' + impexSubSet;

        logMessage("prodMediaCleanupScript | Importing Impex Set ",setCounter);

        importImpex(impexSubSet);
        logMessage("prodMediaCleanupScript | Completed Set ${setCounter} for impex ${targetFileName}\n",null)
        writeSetLog(setCounter)


    }

    logMessage("prodMediaCleanupScript | Import Completed For Impex "+targetFileName,null);
    logLineSeparator();
}


def importImpex(impexContent)
{
    try
    {
        ImportConfig config = new ImportConfig();
        config.setLegacyMode(Boolean.FALSE);
        config.setScript(impexContent);

        ImportResult result = importService.importData(config);

        if (result.isSuccessful())
        {
            logMessage("prodMediaCleanupScript | Impex import was successful.",null);
        }
        else
        {
            logMessage("prodMediaCleanupScript | Impex import failed: " + result.getUnresolvedLines(),null);
            addUnresolvedLinesToLog(result.getUnresolvedLines());
        }

    }
    catch(Exception e)
    {
        logMessage("prodMediaCleanupScript | exception in importImpex ${e.message}",null)
    }


}



def readLocalFileContent(String filePath)
{
    //print message reading content of local file...
    logMessage("prodMediaCleanupScript | Reading content of local file...",null);
    //surround the file reading code with try-catch block to handle any exception
    try
    {
        //read the content of the file using the file path
        def fileContent = new File(filePath).text
        //return the file content
        return fileContent
    }
    catch (Exception e)
    {
        //print the exception message
        logMessage("prodMediaCleanupScript | Exception occurred while reading content of local file: ${e.message}",null);
        return null;
    }
}

def readContentOfBlob(blobFilePath)
{

    def blobClient = establishBlobConnection(azureAccountName, azureSasToken)

    logMessage("prodMediaCleanupScript | connection established "+blobClient,null);

    //get the container reference from the blob client
    def container = blobClient.getContainerReference(azureContainerName)

    //get the directory reference from the container

    //read the contnet of blob from the directory by blob name
    def blob = container.getBlockBlobReference(blobFilePath)

    //hold the content of the blob in a variable
    def blobContent = blob.downloadText()

    return blobContent;
}





def establishBlobConnection(String accountName, String sasToken)
{
    //print message establishing connection with azure blob storage...
    logMessage("Establishing connection with Azure Blob Storage...",null);

    //surround the connection code with try-catch block to handle any exception
    try
    {
        //create connection string using account name and sas token
        def connectionString = "DefaultEndpointsProtocol=https;AccountName=${accountName};SharedAccessSignature=${sasToken}"
        //parse the connection string to create a CloudStorageAccount object
        def storageAccount = CloudStorageAccount.parse(connectionString)
        //create a CloudBlobClient object using the CloudStorageAccount object
        def blobClient = storageAccount.createCloudBlobClient()
        //return the CloudBlobClient object
        return blobClient
    }
    catch (Exception e)
    {
        logMessage("Exception occurred while establishing connection with Azure Blob Storage: ${e.message}",null);
        return null
    }

    return blobClient
}



def logMessage(message,number= null)
{
    if(number != null)
    {
        Formatter fmt = new Formatter();
        fmt.format("%-60s%-15s", message, number);
        println fmt;
    }
    else
    {
        println message;
    }

    String msg = "CleanMediaScript | "+message;
    if(number != null)
    {
        msg = msg+" ==> "+number;
    }
    logger.warn (msg);
    this.CLEANUP_LOG_BUFFER.add(msg)
}

def logLineSeparator()
{
    println "____________________________________________________";
}


def addUnresolvedLinesToLog(impexMedia)
{
    int unresolvedLinesCount=0;
    try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mediaService.getStreamFromMedia(
            impexMedia)));)
    {

        logMessage("============================================================",null);
        logMessage("  Unresolved Lines ",null);
        logMessage("============================================================",null);

        String currentLine = bufferedReader.readLine();

        while (currentLine != null)
        {
            logMessage(currentLine+"\n",null);
            currentLine = bufferedReader.readLine();
            unresolvedLinesCount=unresolvedLinesCount+1;
        }
        logMessage("Unresolved Lines/dumped:"+(unresolvedLinesCount-2),null);
    }
    catch (final Exception e)
    {
        logMessage("Exception in addUnresolvedLinesToLog: ${e.message}",null);
    }
}

def writeSetLog(int setCounter) {

    String fileName = "log_${page_num}_${file_num}_${setCounter}.log"
    String logContent = CLEANUP_LOG_BUFFER.join("\n") + "\n"

    if (TRANSFER_PROTOCOL == "LOCAL") {

        def dir = localPath + pageNumPath
        new File(dir).mkdirs()

        def f = new File(dir + fileName)
        f.text = logContent

        logMessage("Log file written at: " + f.absolutePath)
    }

    if (TRANSFER_PROTOCOL == "BLOB") {

        String remotePath = blobPath + pageNumPath + fileName

        StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(azureSasToken)
        CloudStorageAccount account =
                new CloudStorageAccount(creds, true, null, azureAccountName)
        CloudBlobClient client = account.createCloudBlobClient()
        CloudBlobContainer container = client.getContainerReference(azureContainerName)

        CloudAppendBlob appendBlob = container.getAppendBlobReference(remotePath)
        appendBlob.createOrReplace()
        appendBlob.appendText(logContent)

        logMessage("Blob log written at: " + remotePath)
    }


}

def writeGroupedLog(int setCounter) {

    // Group sets in blocks of 10
    int groupNumber = ((setCounter - 1) / 10) + 1

    String fileName = "log_${page_num}_${file_num}_group_${groupNumber}.log"
    String logContent = CLEANUP_LOG_BUFFER.join("\n") + "\n"

    if (TRANSFER_PROTOCOL == "LOCAL") {

        def dir = localPath + pageNumPath
        new File(dir).mkdirs()

        def f = new File(dir + fileName)
        f.text = logContent

        logMessage("Grouped log written at: " + f.absolutePath)
    }

    if (TRANSFER_PROTOCOL == "BLOB") {

        String remotePath = blobPath + pageNumPath + fileName

        StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(azureSasToken)
        CloudStorageAccount account =
                new CloudStorageAccount(creds, true, null, azureAccountName)
        CloudBlobClient client = account.createCloudBlobClient()
        CloudBlobContainer container = client.getContainerReference(azureContainerName)

        CloudAppendBlob appendBlob = container.getAppendBlobReference(remotePath)
        appendBlob.createOrReplace()
        appendBlob.appendText(logContent)

        logMessage("Grouped blob log written at: " + remotePath)
    }
}

 