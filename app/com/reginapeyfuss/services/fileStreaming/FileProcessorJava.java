package com.reginapeyfuss.services.fileStreaming;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.alpakka.file.DirectoryChange;
import akka.stream.alpakka.file.javadsl.DirectoryChangesSource;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Path;
import java.nio.file.Paths;


public class FileProcessorJava {

    private static final Config config = ConfigFactory.load();
    private static final String  defaultDirConfig = "file-processor.data-dir";
    private static final String dataDir = config.getString(defaultDirConfig);

    private static final String defaultIntervalConfig = "file-processor.interval";
    private static final String polIntervalStr = config.getString(defaultIntervalConfig);
    private static final FiniteDuration polInterval = FiniteDuration.apply(Long.parseLong(polIntervalStr.split(".")[0]), polIntervalStr.split(".")[1]);

    private static final Source<Pair<Path, DirectoryChange>, NotUsed> newFiles = DirectoryChangesSource.create(Paths.get(dataDir), polInterval, 128);

    public Source<String, NotUsed> liveProcessingFiles () {
        return newFiles
                .via(gzJsonPaths)
                .via(fileBytes)
                .via(decompressGZip)
                .via(splitByNewLine);
    }

    private final Flow<Pair<Path, DirectoryChange>, Path, NotUsed> gzJsonPaths =
        Flow.<Pair<Path, DirectoryChange>>create()
            .filter(this::isJsonGZipEvent)
            .log("new file", p -> p.first().getFileName())
        	.map(Pair::first);


    private boolean isJsonGZipEvent (Pair<Path, DirectoryChange> p) {
        return p.first().toString().endsWith("json.gz") && p.second().equals(DirectoryChange.Creation);
    }

    private final Flow<Path, ByteString, NotUsed> fileBytes =
        Flow.of(Path.class).flatMapConcat( FileIO::fromPath);

    private final Flow<ByteString, ByteString, NotUsed> decompressGZip =
        Flow.of(ByteString.class).via(Compression.gzip());


    private final Flow<ByteString, String, NotUsed> splitByNewLine =
        Flow.of(ByteString.class)
                .via(Framing.delimiter(ByteString.fromString("\n"),  Integer.MAX_VALUE, FramingTruncation.ALLOW ))
                .map(p -> p.utf8String());

}
