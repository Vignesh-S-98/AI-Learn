import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import java.util.*;
import java.lang.String;
import java.lang.Long;
import org.apache.log4j.Logger;
import java.lang.Math.*;
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

import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus


try
{

this.EXTRACTION_LOG_BUFFER = []
logger = Logger.getLogger("MediaUtil.class");
if (spring == null) {
    throw new IllegalStateException("Spring context is null in script execution context")
}
logMessage("Initializing flexibleSearchService...", null)
flexibleSearchService = spring.getBean("flexibleSearchService")
logMessage("flexibleSearchService initialized", null)
if (flexibleSearchService == null) {
    throw new IllegalStateException("flexibleSearchService bean not found")
}

/** please change TRANSFER_PROTOCOL and its relevent variales wrt enviroment **/
TRANSFER_PROTOCOL = "BLOB"; //SFTP OR BLOB OR LOCAL


TENANT = "nln" //Update based on the tenant


BLOB_ROOT_DIRECTORY = "/media-extract-test/NLN-Extract-test/";
BLOB_DESTINATION_DIRECTORY = "";
SAS_TOKEN = "sv=2024-11-04&ss=b&srt=sco&sp=rwdlacyx&se=2027-03-16T18:41:17Z&st=2026-03-16T10:26:17Z&spr=https&sig=cvrtSOWBsCTiQr5PnefrNKPi%2FYjCIP8YgmFaken0%2BQE%3D";
AZURE_ACCOUNT_NAME = "starxlhquathybcaas01"
AZURE_CONTAINER_NAME = "dataload";


LOCAL_FOLDER_ROOT_DIRECTORY = "";
LOCAL_DESTINATION_DIRECTORY = "";

SFTP_ROOT_DIRECTORY = "/media-extract/";
SFTP_DESTINATION_DIRECTORY = "";


FILE_NAME = "";
FILE_EXTENSION = ".impex"; //.impex or .txt
FILE_HEADER = "";

if (FILE_EXTENSION == ".impex") {
    FILE_HEADER = "Remove Media;pk[unique=true];location\n";
}
def startTime = new Date();
logMessage("Product media analysis script started...", null);
logLineSeparator();
totalProductCount = totalProductCount();
totalMediaCount = totalMediaCount();
limitedProductPkPageNumber = 1;
limitedProductPkPageSize = 40000;
fileThreshold = 40000;
queryThresholdInClause = 10000;
totalProductPages = Math.ceil(totalProductCount / (double) limitedProductPkPageSize).intValue()
logMessage("Total proudct pages to traverse ", totalProductPages);
LOG_FILE_NAME = "log_media_extraction"

while (limitedProductPkPageNumber <= totalProductPages) {

    logMessage("Processing product page ", limitedProductPkPageNumber);
    if (TRANSFER_PROTOCOL == "SFTP") {
        SFTP_DESTINATION_DIRECTORY = SFTP_ROOT_DIRECTORY + "page-" + limitedProductPkPageNumber + "/";
    }
    if (TRANSFER_PROTOCOL == "BLOB") {
        BLOB_DESTINATION_DIRECTORY = "/page-" + limitedProductPkPageNumber + "/";
    }
    if (TRANSFER_PROTOCOL == "LOCAL") {
        LOCAL_DESTINATION_DIRECTORY = "/page-" + limitedProductPkPageNumber + "/";
    }


    limitedProductSetPks = getLimitedNumberOfProductsPk();
    thumbnailMediaPK_Prod = getProductThumbnail();
    pictureMediaPK_Prod = getProductPicture();
    allMediaInMediaCollectionPkList_Prod = getProductMediaPkFromMediaCollection();
    allMediaPkInsideContainer_Prod = getProductMediaPkFromMediaContainer();
    //consolidating all product medias pk
    Set<String> allMediaAttributesPKRefAgainstProducts = [];
    allMediaAttributesPKRefAgainstProducts.addAll(thumbnailMediaPK_Prod);
    allMediaAttributesPKRefAgainstProducts.addAll(pictureMediaPK_Prod);
    allMediaAttributesPKRefAgainstProducts.addAll(allMediaInMediaCollectionPkList_Prod);
    allMediaAttributesPKRefAgainstProducts.addAll(allMediaPkInsideContainer_Prod);
    allMediaAttributesPKRefAgainstProducts.removeAll([null]);
    allMediaAttributesPKRefAgainstProducts.removeAll { it.isEmpty() || it.trim().isEmpty() }
    allMediaAttributesPKRefAgainstProducts.removeAll { it.startsWith('#') }
    logMessage("All Distinct Product Medias", allMediaAttributesPKRefAgainstProducts.size());
    logLineSeparator();
    FILE_NAME = "m_" + TENANT + "_ProductMediasPkAndUrls_" + limitedProductPkPageNumber;
    if (allMediaAttributesPKRefAgainstProducts.size() > 0) {
//        logMessage('"DEBUG PK sample: " + allMediaAttributesPKRefAgainstProducts.take(20)',null)
        prepareMediaContent(allMediaAttributesPKRefAgainstProducts, FILE_NAME);
        FILE_HEADER = "";
        prepareProductsWithProductCode();
    }
    writeExtractionLog(this.EXTRACTION_LOG_BUFFER, LOG_FILE_NAME)

    limitedProductPkPageNumber = limitedProductPkPageNumber + 1;
    SFTP_DESTINATION_DIRECTORY = "";
    BLOB_DESTINATION_DIRECTORY = "";
    LOCAL_DESTINATION_DIRECTORY = "";
    FILE_HEADER = "Remove Media;pk[unique=true];location\n";

    EXTRACTION_LOG_BUFFER.clear()
}


def endTime = new Date();
// Calculate the duration
TimeDuration duration = TimeCategory.minus(endTime, startTime);
logMessage("Duration of script execution : $duration", null);


return [CronJobResult.SUCCESS, CronJobStatus.FINISHED]
}
catch (Exception e)
{
    logger.error("ProdMediaExtractScript | FATAL ERROR", e)

    try
    {
        writeExtractionLog(this.EXTRACTION_LOG_BUFFER, "log_media_extraction")
    }
    catch (ignored) {}

return [ CronJobResult.ERROR, CronJobStatus.ABORTED]
}

def totalMediaCount() {
    //find total number of medias in media table
    totalMediaCountQuery = "select count( distinct {pk}) from {Media}";
    FlexibleSearchQuery totalMediaCountFSQ = new FlexibleSearchQuery(totalMediaCountQuery);
    totalMediaCountFSQ.setResultClassList(Collections.singletonList(Long.class));
    totalMediaCountResult = flexibleSearchService.search(totalMediaCountFSQ);
    totalMediaCount = totalMediaCountResult.getResult().get(0);
    logMessage("Number Media in media table", totalMediaCount);
    logLineSeparator();
    return totalMediaCount;
}

def totalProductCount() {
    //find total number of products
    totalProductCountQuery = "select count( distinct {pk}) from {Product}";
    FlexibleSearchQuery prodCountFSQ = new FlexibleSearchQuery(totalProductCountQuery);
    prodCountFSQ.setResultClassList(Collections.singletonList(Long.class));
    totalProductCountResult = flexibleSearchService.search(prodCountFSQ);
    totalProductsCount = totalProductCountResult.getResult().get(0);
    logMessage("Total Product Count", totalProductsCount);
    logLineSeparator();
    return totalProductsCount;
}

def getLimitedNumberOfProductsPk() {
    //find limited product PKs
    limitedProductPkStartIndex = (limitedProductPkPageNumber - 1) * limitedProductPkPageSize;
    limitedProductSetQuery = "select distinct {pk} from {Product}";
    FlexibleSearchQuery limitedProductSetQueryFSQ = new FlexibleSearchQuery(limitedProductSetQuery);
    limitedProductSetQueryFSQ.setStart(limitedProductPkStartIndex);
    limitedProductSetQueryFSQ.setCount(limitedProductPkPageSize);
    limitedProductSetQueryFSQ.setResultClassList(Collections.singletonList(String.class));
    limitedProductSetQueryFSQResult = flexibleSearchService.search(limitedProductSetQueryFSQ);
    limitedProductSetPks = limitedProductSetQueryFSQResult.getResult();
    logMessage("Product[pk] limitedProductPkPageNumber", limitedProductPkPageNumber);
    logMessage("Product[pk] limitedProductPkStartIndex", limitedProductPkStartIndex);
    logMessage("Product[pk] limitedProductPkPageSize", limitedProductPkPageSize);
    logMessage("Product[pk] found", limitedProductSetPks.size());
    logLineSeparator();
    return limitedProductSetPks;
}

def getProductThumbnail() {
    //find all [thumbnail] media against product
    thumbnail_Query_Prod = "select distinct {thumbnail} from {Product} where {pk} IN (?productPK)";
    thumbnailMediaPK_Prod = executeInQueryBatchWise(thumbnail_Query_Prod, "productPK");
    logMessage("Product[thumbnail] medias", thumbnailMediaPK_Prod.size());
    logLineSeparator();
    return thumbnailMediaPK_Prod;
}

def getProductPicture() {
    //find all [picture] media against product
    picture_Query_Prod = "select distinct {picture} from {Product} where {pk} IN (?productPK)";
    pictureMediaPK_Prod = executeInQueryBatchWise(picture_Query_Prod, "productPK");
    logMessage("Product[picture] medias", pictureMediaPK_Prod.size());
    logLineSeparator();
    return pictureMediaPK_Prod;
}

def getProductMediaPkFromMediaCollection() {
    //find all ['normal','thumbnails','detail','logo','data_sheet','others','videos','mobileImages'] media against product
    Set<String> allMediaInMediaCollectionPkList_Prod = [];
    productMediaInMediaCollectionFields = ['normal', 'thumbnails', 'detail', 'logo', 'data_sheet', 'others', 'videos', 'mobileImages']
    for (attribute in productMediaInMediaCollectionFields) {
        mediaCollectionQuery_Prod = "select {" + attribute + "} from {Product} where {pk} IN (?productPK)";
        Set<String> allMediaInMediaCollectionPkListTemp_Prod = [];
        productMediaInMediaCollectionResultData = executeInQueryBatchWise(mediaCollectionQuery_Prod, "productPK");
        for (attr in productMediaInMediaCollectionResultData) {
            if (attr?.trim()) {
                def pkArrList = attr.split(',') as Set
                allMediaInMediaCollectionPkListTemp_Prod.addAll(pkArrList);
            }

        }

        logMessage("Product[" + attribute + "] medias", allMediaInMediaCollectionPkListTemp_Prod.size());

        allMediaInMediaCollectionPkList_Prod.addAll(allMediaInMediaCollectionPkListTemp_Prod);
        allMediaInMediaCollectionPkListTemp_Prod = [];
    }
    logLineSeparator();
    logMessage("Product medias in media collection", allMediaInMediaCollectionPkList_Prod.size());
    logLineSeparator();
    return allMediaInMediaCollectionPkList_Prod;
}

def getProductMediaPkFromMediaContainer() {
    //find all [galleryImages] media against product
    Set<String> allMediaContainerPkList_Prod = [];
    //finding medias container references against products
    //CLOUD-54481: Define process for Product medias clean up in Hybris
    mediaContainerAgainstProductQuery_Prod = "select {galleryImages} from {Product} where {pk} IN (?productPK)";
    mediaContainerAgainstProductResultData = executeInQueryBatchWise(mediaContainerAgainstProductQuery_Prod, "productPK");
    for (gallery in mediaContainerAgainstProductResultData) {
        if (gallery?.trim()) {
            def pkArrList = gallery.split(',') as Set
            allMediaContainerPkList_Prod.addAll(pkArrList);
        }

    }
    logMessage("Product[galleryImages] media containers", allMediaContainerPkList_Prod.size());
    //finding medias inside container
    totalMediaContainerRefCount = allMediaContainerPkList_Prod.size();
    allMediaPkInsideContainer_Prod = [];
    if (totalMediaContainerRefCount > 0) {
        if (queryThresholdInClause > totalMediaContainerRefCount) {
            queryThresholdInClause = totalMediaContainerRefCount;
        }
        pageSize = queryThresholdInClause
        currentPage = 1
        totalPages = Math.ceil(totalMediaContainerRefCount / (double) pageSize).intValue()


        mediaInContainerQuery_Prod = "SELECT {med.pk} FROM { MediaContainer AS cont JOIN Media AS med ON {med.mediaContainer} = {cont.pk} }" +
                "WHERE {cont.pk} IN (?containerPKparam)"
        while (currentPage <= totalPages) {
            def startIndex = (currentPage - 1) * pageSize;
            def endIndex = (currentPage == totalPages) ? totalMediaContainerRefCount : (startIndex + pageSize);
            def paramval = allMediaContainerPkList_Prod.asList().subList(startIndex, endIndex);

            FlexibleSearchQuery flexibQuery_Prod = new FlexibleSearchQuery(mediaInContainerQuery_Prod);
            flexibQuery_Prod.addQueryParameter("containerPKparam", paramval);
            flexibQuery_Prod.setResultClassList(Collections.singletonList(String.class));
            mediaInContainerResult_Prod = flexibleSearchService.search(flexibQuery_Prod);
            mediaPkInContainer_Prod = mediaInContainerResult_Prod.getResult();
            allMediaPkInsideContainer_Prod.addAll(mediaPkInContainer_Prod);
            currentPage = currentPage + 1;

        }
        allMediaPkInsideContainer_Prod.removeAll { it.isEmpty() || it.trim().isEmpty() }
        allMediaPkInsideContainer_Prod.removeAll { it.startsWith('#') }
        logMessage("Product[galleryImages] medias", allMediaPkInsideContainer_Prod.size());
        logLineSeparator();
    }

    return allMediaPkInsideContainer_Prod;
}

def prepareMediaContent(allPkList, fileName) {
    Set<String> missingMediaPkList = [];
    if (queryThresholdInClause > allPkList.size()) {
        queryThresholdInClause = allPkList.size();
    }
    fileCounter = 1;
    Set<String> mediaUrlContent = [];
    String mediaDataDir = de.hybris.platform.util.MediaUtil.getLocalStorageDataDir() ?: "";
    //CLOUD-54481: Define process for Product medias clean up in Hybris
    mediaUrlQuery = "SELECT {m.pk}, {m.location}, {c.pk} FROM {Media AS m LEFT JOIN MediaContainer AS c ON {m.mediaContainer} = {c.pk}} WHERE {m.pk} IN (?mediaPK)";
    pageSize = queryThresholdInClause
    currentPage = 1

    totalPages = Math.ceil(allPkList.size() / (double) pageSize).intValue()
    while (currentPage <= totalPages) {
        def startIndex = (currentPage - 1) * pageSize;
        def endIndex = (currentPage == totalPages) ? allPkList.size() : (startIndex + pageSize);
        def pktempList = allPkList.asList().subList(startIndex, endIndex);

        def mediaPkAndUrl;
        FlexibleSearchQuery mediaUrlQueryFSQ = new FlexibleSearchQuery(mediaUrlQuery);
        mediaUrlQueryFSQ.addQueryParameter("mediaPK", pktempList);
        mediaUrlQueryFSQ.setResultClassList(Arrays.asList(String.class, String.class));
        mediaUrlQueryResult = flexibleSearchService.search(mediaUrlQueryFSQ);
        mediaPkAndUrl = mediaUrlQueryResult.getResult();
        Set<String> tempList = [];
        for (row in mediaPkAndUrl) {
            tempList.add(row.get(0));
        }
        missingMediaPkList.addAll(pktempList - tempList);

        for (d in mediaPkAndUrl) {
            mediaUrlContent.add(d.get(0) + ';' + mediaDataDir + '/' + d.get(1));
        }
        if ((mediaUrlContent.size() >= fileThreshold) || (currentPage == totalPages)) {
            logMessage("Media Pk and Url content is ready to be written on file no " + fileCounter + " rows " + mediaUrlContent.size(), null);
            if (TRANSFER_PROTOCOL == "SFTP") {
                writeMediaDetailsToSftp(mediaUrlContent, fileName, fileCounter);
            }
            if (TRANSFER_PROTOCOL == "BLOB") {
                writeMediaDetailsToBLOB(mediaUrlContent, fileName, fileCounter);
            }
            if (TRANSFER_PROTOCOL == "LOCAL") {
                writeMediaDetailsToLocal(mediaUrlContent, fileName, fileCounter);
            }

            fileCounter = fileCounter + 1;
            mediaUrlContent.clear();
        }
        currentPage = currentPage + 1;

    }
    logMessage("Number of product media references not found in media table", missingMediaPkList.size());
    logLineSeparator();
}

def writeMediaDetailsToSftp(contentLines, fileName, fileCounter) {
    def fileWriteThroshold = 5000;
    def updatedContentLines = contentLines.collect { ';' + it + '\n' };
    def pagedContentLines = updatedContentLines.collate(fileWriteThroshold);
    String user = '';
    String host = '';
    String passwd = '';
    String remoteFilePath = SFTP_DESTINATION_DIRECTORY + fileName + "-" + fileCounter + FILE_EXTENSION;
    try {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setConfig('StrictHostKeyChecking', 'no');
        session.setPassword(passwd);
        session.connect();
        Channel channel = session.openChannel('sftp')
        channel.connect();
        ChannelSftp sftpChannel = channel as ChannelSftp
        try {
            sftpChannel.cd(SFTP_DESTINATION_DIRECTORY);
        }
        catch (Exception e) {
            // Directory does not exist, create it
            sftpChannel.mkdir(SFTP_DESTINATION_DIRECTORY);
        }
        //impex header
        sftpChannel.put(new ByteArrayInputStream(FILE_HEADER.getBytes()), remoteFilePath);
        pagedContentLines.each { page ->

            String contentToAppend = page.join('');
            OutputStream outputStream = sftpChannel.put(remoteFilePath, null, ChannelSftp.APPEND);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(contentToAppend.getBytes());
            bufferedOutputStream.close();
        }
        logMessage("Media Extract File Created " + fileName + "-" + fileCounter, null);
        logLineSeparator();
        sftpChannel.exit();
        session.disconnect();
    }
    catch (JSchException | SftpException e) {
        logMessage("prodMediaScript | Error during SFTP operation: ${e.message}", null);
    }

}

def writeMediaDetailsToBLOB(contentLines, fileName, fileCounter) {
    def fileWriteThroshold = 5000;
    def isLogFile = (fileName == LOG_FILE_NAME)
    def extension = isLogFile ? ".log" : FILE_EXTENSION
    def updatedContentLines = isLogFile ? contentLines.collect { it + '\n' } : contentLines.collect { ';' + it + '\n' }
    def pagedContentLines = updatedContentLines.collate(fileWriteThroshold);

    String azureSasToken = SAS_TOKEN
    String azureAccountName = AZURE_ACCOUNT_NAME
    String azureContainerName = AZURE_CONTAINER_NAME

    String remoteFilePath = BLOB_ROOT_DIRECTORY + BLOB_DESTINATION_DIRECTORY + fileName + "-" + fileCounter + extension;
    try {
        StorageCredentials storageCredentials = new StorageCredentialsSharedAccessSignature(azureSasToken);
        CloudStorageAccount storageAccount = new CloudStorageAccount(storageCredentials, true, null,
                azureAccountName);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(azureContainerName);
        CloudAppendBlob appendBlob = container.getAppendBlobReference(remoteFilePath);
        if (!appendBlob.exists()) {
            appendBlob.createOrReplace();
        }

        //impex header
        if (!isLogFile && FILE_HEADER != "") {
            appendBlob.appendText(FILE_HEADER)
        }

        pagedContentLines.each { page ->

            String contentToAppend = page.join('');
            appendBlob.appendText(contentToAppend);
        }

        logMessage("Media Extract File Created " + fileName + "-" + fileCounter, null);
        logLineSeparator();
    }
    catch (Exception e) {
        logMessage("prodMediaScript | Error during BLOB operation: ${e.message}", null);
    }

}

def writeMediaDetailsToLocal(contentLines, fileName, fileCounter) {
    def fileWriteThroshold = 5000;
    def isLogFile = (fileName == LOG_FILE_NAME)
    def extension = isLogFile ? ".log" : FILE_EXTENSION
    def updatedContentLines = isLogFile ? contentLines.collect { it + '\n' } : contentLines.collect { ';' + it + '\n' }
    def pagedContentLines = updatedContentLines.collate(fileWriteThroshold);
    String remoteFilePath = LOCAL_FOLDER_ROOT_DIRECTORY + LOCAL_DESTINATION_DIRECTORY + fileName + "-" + fileCounter + extension;
    def directoryPath = LOCAL_FOLDER_ROOT_DIRECTORY + LOCAL_DESTINATION_DIRECTORY;
    def directory = new File(directoryPath);
    if (!directory.exists()) {
        directory.mkdirs();
    }
    try {
        def file = new File(remoteFilePath);

        //impex header
        if (!isLogFile && FILE_HEADER != "") {
            file.append(FILE_HEADER);
        }

        pagedContentLines.each { page ->

            String contentToAppend = page.join('');
            file.append(contentToAppend);
        }

        logMessage("Media Extract File Created " + fileName + "-" + fileCounter, null);
        logLineSeparator();
    }
    catch (Exception e) {
        logMessage("prodMediaScript | Error during Local file operation: ${e.message}", null);
    }

}

def executeInQueryBatchWise(queryString, queryParamName) {
    Set<String> uniquePkList = [];
    if (queryThresholdInClause > limitedProductPkPageSize) {
        queryThresholdInClause = limitedProductPkPageSize;
    }
    pageSize = queryThresholdInClause;
    currentPage = 1;

    totalPages = Math.ceil(limitedProductSetPks.size() / (double) pageSize).intValue()

    while (currentPage <= totalPages) {
        def startIndex = (currentPage - 1) * pageSize;
        def endIndex = (currentPage == totalPages) ? limitedProductSetPks.size() : (startIndex + pageSize);
        def pktempList = limitedProductSetPks.asList().subList(startIndex, endIndex);

        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(queryString);
        flexibleSearchQuery.addQueryParameter(queryParamName, pktempList);
        flexibleSearchQuery.setResultClassList(Collections.singletonList(String.class));
        flexibleSearchQueryResult = flexibleSearchService.search(flexibleSearchQuery);
        flexibleSearchQueryResultData = flexibleSearchQueryResult.getResult();
        uniquePkList.addAll(flexibleSearchQueryResultData);
        currentPage = currentPage + 1;
    }
    uniquePkList.removeAll([null]);
    uniquePkList.removeAll { it.isEmpty() || it.trim().isEmpty() }
    uniquePkList.removeAll { it.startsWith('#') }
    return uniquePkList;
}


def logMessage(message, number = null) {
    if (number != null) {
        Formatter fmt = new Formatter();
        fmt.format("%-60s%-15s", message, number);
        println fmt;
    } else {
        println message;
    }

    String msg = "ProdMediaExtractScript | " + message;
    if (number != null) {
        msg = msg + " ==> " + number;
    }
    logger.warn(msg);

    EXTRACTION_LOG_BUFFER.add(msg);
}


def writeExtractionLog(List<String> logLines, String fileName) {

    if (TRANSFER_PROTOCOL == "SFTP") {
        writeMediaDetailsToSftp(logLines, fileName, 1)   // Always fileCounter = 1
    }

    if (TRANSFER_PROTOCOL == "BLOB") {
        writeMediaDetailsToBLOB(logLines, fileName, 1)
    }

    if (TRANSFER_PROTOCOL == "LOCAL") {
        writeMediaDetailsToLocal(logLines, fileName, 1)
    }
}

def logLineSeparator() {
    println "____________________________________________________";
}

def prepareProductsWithProductCode() {
    productPkWithCodeQuery = "SELECT {pk},{code} FROM {Product} WHERE {pk} IN (?productPK)";
    Set<String> productCodeContent = [];
    if (queryThresholdInClause > limitedProductPkPageSize) {
        queryThresholdInClause = limitedProductPkPageSize;
    }
    pageSize = queryThresholdInClause;
    currentPage = 1;

    totalPages = Math.ceil(limitedProductSetPks.size() / (double) pageSize).intValue()

    fileCounter = 1;
    while (currentPage <= totalPages) {
        //println "currentPage > "+currentPage;
        def startIndex = (currentPage - 1) * pageSize;
        def endIndex = (currentPage == totalPages) ? limitedProductSetPks.size() : (startIndex + pageSize);
        def paramval = limitedProductSetPks.asList().subList(startIndex, endIndex);

        FlexibleSearchQuery productPkWithCodeFSQ = new FlexibleSearchQuery(productPkWithCodeQuery);
        productPkWithCodeFSQ.addQueryParameter("productPK", paramval);
        productPkWithCodeFSQ.setResultClassList(Arrays.asList(String.class, String.class));
        productPkWithCodeFSQResult = flexibleSearchService.search(productPkWithCodeFSQ);
        productPkWithCodeFSQResultData = productPkWithCodeFSQResult.getResult();
        for (d in productPkWithCodeFSQResultData) {
            productCodeContent.add(d.get(0) + ';' + d.get(1));
        }
        if ((productCodeContent.size() >= fileThreshold) || (currentPage == totalPages)) {

            logMessage("productCodeContent ready ", null);
            String fileName = "products_" + limitedProductPkPageNumber + "_" + limitedProductPkStartIndex + "_" + getCurrentDate();


            if (TRANSFER_PROTOCOL == "SFTP") {
                writeMediaDetailsToSftp(productCodeContent, fileName, fileCounter);
            }
            if (TRANSFER_PROTOCOL == "BLOB") {
                writeMediaDetailsToBLOB(productCodeContent, fileName, fileCounter);
            }
            if (TRANSFER_PROTOCOL == "LOCAL") {
                writeMediaDetailsToLocal(productCodeContent, fileName, fileCounter);
            }
            fileCounter = fileCounter + 1;
            productCodeContent.clear();
        }
        currentPage = currentPage + 1;

    }
}

def getCurrentDate() {
    def date = new Date();
    def sdf = new SimpleDateFormat("dd-MMM-yyy-hh-mm");
    return sdf.format(date);
}

