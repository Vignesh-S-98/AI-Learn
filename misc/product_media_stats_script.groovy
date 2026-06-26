import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import org.apache.log4j.Logger
import groovy.time.*
import java.util.*

import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus
import de.hybris.platform.servicelayer.model.ModelService

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature
import com.microsoft.azure.storage.blob.CloudAppendBlob
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer

try
{


    this.STATS_LOG_BUFFER = []

    TRANSFER_PROTOCOL = "BLOB"   // LOCAL or BLOB

    LOCAL_LOG_DIR = ""


    FILE_NAME = "NLN_stats_script_job_check.log"

    azureSasToken = "sv=2024-11-04&ss=b&srt=sco&sp=rwdlacyx&se=2027-03-16T18:41:17Z&st=2026-03-16T10:26:17Z&spr=https&sig=cvrtSOWBsCTiQr5PnefrNKPi%2FYjCIP8YgmFaken0%2BQE%3D"
    azureAccountName = "starxlhquathybcaas01"
    azureContainerName = "dataload"
    azureBlobPath = "media-extract-test/logs/"+FILE_NAME

    logger = Logger.getLogger("MediaUtil.class")
    line_separator = "----------------------------------------------"

    flexibleSearchService = spring.getBean("flexibleSearchService")
    def modelService = spring.getBean("modelService")

    def cronJob = binding.hasVariable('cronJob') ? binding.getVariable('cronJob') : null

    CATEGORY_PAGE_SIZE = 20000
    PRODUCT_PAGE_SIZE  = 20000
    MEDIA_PAGE_SIZE    = 20000

    def startTime = new Date()
    STATS_LOG_BUFFER.add("prodMediaScript | STARTED at " + startTime)
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()


    def catCountFSQ = new FlexibleSearchQuery("select count({pk}) from {Category}")
    catCountFSQ.setResultClassList(Collections.singletonList(Long.class))
    totalCategorysCount = flexibleSearchService.search(catCountFSQ).getResult().get(0)

    STATS_LOG_BUFFER.add("prodMediaScript | Total Categories -> " + totalCategorysCount)
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()

    Set<String> categoryMediaPKs = []

    categoryAttributes =
            ['thumbnail','picture','normal','thumbnails','detail','logo','data_sheet','others']

    for (attribute in categoryAttributes)
    {
        Set<String> attrMediaPKs = []

        pageSize = CATEGORY_PAGE_SIZE
        currentPage = 1
        totalPages = Math.ceil(totalCategorysCount / pageSize)

        while (currentPage <= totalPages)
        {
            def fsq = new FlexibleSearchQuery("select {" + attribute + "} from {Category}")
            fsq.setStart((currentPage - 1) * pageSize)
            fsq.setCount(pageSize)
            fsq.setResultClassList(Collections.singletonList(String.class))

            def result = flexibleSearchService.search(fsq).getResult()

            for (val in result)
            {
                if (val?.trim())
                {
                    val.split(',').each { pk ->
                        if (pk && !pk.startsWith('#'))
                        {
                            attrMediaPKs.add(pk.trim())
                        }
                    }
                }
            }

            logger.warn("prodMediaScript | CATEGORY | " + attribute +
                    " | page " + currentPage + "/" + totalPages +
                    " | refs " + attrMediaPKs.size())
            currentPage++
        }

        categoryMediaPKs.addAll(attrMediaPKs)

        STATS_LOG_BUFFER.add("prodMediaScript | CATEGORY | " + attribute + " -> " + attrMediaPKs.size())
        println STATS_LOG_BUFFER.last()
        logger.warn(STATS_LOG_BUFFER.last())
        writeSetLog()

        attrMediaPKs = null
    }



    def prodCountFSQ = new FlexibleSearchQuery("select count({pk}) from {Product}")
    prodCountFSQ.setResultClassList(Collections.singletonList(Long.class))
    totalProductsCount = flexibleSearchService.search(prodCountFSQ).getResult().get(0)

    STATS_LOG_BUFFER.add("prodMediaScript | Total Products -> " + totalProductsCount)
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()

    Set<String> productMediaPKs = []

    productAttributes =
            ['thumbnail','picture','normal','thumbnails','detail','logo','data_sheet',
             'others','videos','mobileImages']

    for (attribute in productAttributes)
    {
        Set<String> attrMediaPKs = []

        pageSize = PRODUCT_PAGE_SIZE
        currentPage = 1
        totalPages = Math.ceil(totalProductsCount / pageSize)

        while (currentPage <= totalPages)
        {
            def fsq = new FlexibleSearchQuery("select {" + attribute + "} from {Product}")
            fsq.setStart((currentPage - 1) * pageSize)
            fsq.setCount(pageSize)
            fsq.setResultClassList(Collections.singletonList(String.class))

            def result = flexibleSearchService.search(fsq).getResult()

            for (val in result)
            {
                if (val?.trim())
                {
                    val.split(',').each { pk ->
                        if (pk && !pk.startsWith('#'))
                        {
                            attrMediaPKs.add(pk.trim())
                        }
                    }
                }
            }

            logger.warn("prodMediaScript | PRODUCT | " + attribute +
                    " | page " + currentPage + "/" + totalPages +
                    " | refs " + attrMediaPKs.size())
            currentPage++
        }

        productMediaPKs.addAll(attrMediaPKs)

        STATS_LOG_BUFFER.add("prodMediaScript | PRODUCT | " + attribute + " -> " + attrMediaPKs.size())
        println STATS_LOG_BUFFER.last()
        logger.warn(STATS_LOG_BUFFER.last())
        writeSetLog()

        attrMediaPKs = null
    }



    Set<String> productGalleryContainerPKs = []

    pageSize = PRODUCT_PAGE_SIZE
    currentPage = 1
    totalPages = Math.ceil(totalProductsCount / pageSize)

    while (currentPage <= totalPages)
    {
        def fsq = new FlexibleSearchQuery("select {galleryImages} from {Product}")
        fsq.setStart((currentPage - 1) * pageSize)
        fsq.setCount(pageSize)
        fsq.setResultClassList(Collections.singletonList(String.class))

        def result = flexibleSearchService.search(fsq).getResult()

        result.each { val ->
            if (val?.trim())
            {
                val.split(',').each { pk ->
                    if (pk && !pk.startsWith('#'))
                    {
                        productGalleryContainerPKs.add(pk.trim())
                    }
                }
            }
        }

        currentPage++
    }

    STATS_LOG_BUFFER.add("prodMediaScript | PRODUCT | galleryImages containers -> " + productGalleryContainerPKs.size())
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()



    Set<String> galleryMediaPKs = []

    if (!productGalleryContainerPKs.isEmpty())
    {
        pageSize = 20000
        currentPage = 1
        totalPages = Math.ceil(productGalleryContainerPKs.size() / pageSize)

        while (currentPage <= totalPages)
        {
            int startIdx = (currentPage - 1) * pageSize
            int endIdx = Math.min(startIdx + pageSize, productGalleryContainerPKs.size())

            def subList = productGalleryContainerPKs.toList().subList(startIdx, endIdx)

            def fsq = new FlexibleSearchQuery(
                    "SELECT {med.pk} FROM { MediaContainer AS cont JOIN Media AS med " +
                            "ON {med.mediaContainer} = {cont.pk} } WHERE {cont.pk} IN (?containerPKs)"
            )
            fsq.addQueryParameter("containerPKs", subList)
            fsq.setResultClassList(Collections.singletonList(String.class))

            flexibleSearchService.search(fsq).getResult().each { pk ->
                if (pk && !pk.startsWith('#'))
                {
                    galleryMediaPKs.add(pk.trim())
                }
            }

            currentPage++
        }
    }

    STATS_LOG_BUFFER.add("prodMediaScript | PRODUCT | galleryImages medias -> " + galleryMediaPKs.size())
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()


    Set<String> referencedMediaPKs = []
    referencedMediaPKs.addAll(categoryMediaPKs)
    referencedMediaPKs.addAll(productMediaPKs)
    referencedMediaPKs.addAll(galleryMediaPKs)


  Set<String> allDistinctProductMediaPKs = []
  allDistinctProductMediaPKs.addAll(productMediaPKs)
  allDistinctProductMediaPKs.addAll(galleryMediaPKs)

  STATS_LOG_BUFFER.add("prodMediaScript | ALL DISTINCT PRODUCT MEDIAS -> " + allDistinctProductMediaPKs.size()  )
  println STATS_LOG_BUFFER.last()
  logger.warn(STATS_LOG_BUFFER.last())
  writeSetLog()


    def mediaCountFSQ = new FlexibleSearchQuery("select count({pk}) from {Media}")
    mediaCountFSQ.setResultClassList(Collections.singletonList(Long.class))
    long totalMediaCount = flexibleSearchService.search(mediaCountFSQ).getResult().get(0)


  STATS_LOG_BUFFER.add("prodMediaScript | TOTAL MEDIA COUNT -> " + totalMediaCount)
  println STATS_LOG_BUFFER.last()
  logger.warn(STATS_LOG_BUFFER.last())
  writeSetLog()


    pageSize = MEDIA_PAGE_SIZE
    currentPage = 1
    totalPages = Math.ceil(totalMediaCount / pageSize)

    long unusedMediaCount = 0

    while (currentPage <= totalPages)
    {
        def fsq = new FlexibleSearchQuery("select {pk} from {Media}")
        fsq.setStart((currentPage - 1) * pageSize)
        fsq.setCount(pageSize)
        fsq.setResultClassList(Collections.singletonList(String.class))

        flexibleSearchService.search(fsq).getResult().each { pk ->
            if (!referencedMediaPKs.contains(pk))
            {
                unusedMediaCount++
            }
        }
        currentPage++
    }

    STATS_LOG_BUFFER.add("prodMediaScript | UNUSED MEDIA COUNT -> " + unusedMediaCount)
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()

    def endTime = new Date()
    STATS_LOG_BUFFER.add("prodMediaScript | DURATION -> " + TimeCategory.minus(endTime, startTime))
    println STATS_LOG_BUFFER.last()
    logger.warn(STATS_LOG_BUFFER.last())
    writeSetLog()

    return [CronJobResult.SUCCESS, CronJobStatus.FINISHED]
}
catch (Exception e)
{
    logger.error("prodMediaScript | FATAL ERROR", e)
    try
    {
        writeSetLog()
    }
    catch (ignored) {}

    return [CronJobResult.ERROR, CronJobStatus.ABORTED]
}



def writeSetLog()
{
    String logContent = STATS_LOG_BUFFER.join("\n") + "\n"

    if (TRANSFER_PROTOCOL == "LOCAL")
    {
        new File(LOCAL_LOG_DIR).mkdirs()
        new File(LOCAL_LOG_DIR + FILE_NAME).text = logContent
    }

    if (TRANSFER_PROTOCOL == "BLOB")
    {
        StorageCredentialsSharedAccessSignature creds =
                new StorageCredentialsSharedAccessSignature(azureSasToken)

        CloudStorageAccount account =
                new CloudStorageAccount(creds, true, null, azureAccountName)

        CloudAppendBlob appendBlob =
                account.createCloudBlobClient()
                        .getContainerReference(azureContainerName)
                        .getAppendBlobReference(azureBlobPath)

        if (!appendBlob.exists())
        {
            appendBlob.createOrReplace()
        }

        appendBlob.appendText(logContent)
    }
}