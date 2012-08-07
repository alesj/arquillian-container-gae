package org.jboss.arquillian.container.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;

/**
 * This is a rip-off from ExplodedExporterDelegate. ;-)
 */
class FixedExplodedExporter {
    private static final Logger log = Logger.getLogger(FixedExplodedExporter.class.getName());

    private Archive archive;
    private File outputDirectory;

    FixedExplodedExporter(Archive archive, File root) {
        this.archive = archive;
        this.outputDirectory = initializeOutputDirectory(root, archive.getName());
    }

    File export() {
        doExport();
        return outputDirectory;
    }

    protected void doExport() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Exporting archive - " + archive.getName());
        }

        // Obtain the root
        final Node rootNode = archive.get(ArchivePaths.root());

        // Recursively process the root children
        for (Node child : rootNode.getChildren()) {
            processNode(child);
        }
    }

    /**
     * Recursive call to process all the node hierarchy
     *
     * @param node the node
     */
    protected void processNode(final Node node) {
        processNode(node.getPath(), node);

        Set<Node> children = node.getChildren();
        for (Node child : children) {
            processNode(child);
        }
    }

    protected void processNode(ArchivePath path, Node node) {
        // Get path to file
        final String assetFilePath = path.get();

        // Create a file for the asset
        final File assetFile = new File(outputDirectory, assetFilePath);

        // Get the assets parent parent directory and make sure it exists
        final File assetParent = assetFile.getParentFile();
        if (!assetParent.exists()) {
            if (!assetParent.mkdirs()) {
                throw new IllegalArgumentException("Failed to write asset.  Unable to create parent directory.");
            }
        }

        // Handle directory assets separately
        try {
            final boolean isDirectory = (node.getAsset() == null);
            if (isDirectory) {
                // If doesn't already exist
                if (!assetFile.exists()) {
                    // Attempt a create
                    if (!assetFile.mkdirs()) {
                        // Some error in writing
                        throw new IllegalArgumentException("Failed to write directory: " + assetFile.getAbsolutePath());
                    }
                }
            }
            // Only handle non-directory assets, otherwise the path is handled above
            else {
                try {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Writing asset " + path.get() + " to " + assetFile.getAbsolutePath());
                    }
                    // Get the asset streams
                    final InputStream assetInputStream = node.getAsset().openStream();
                    final FileOutputStream assetFileOutputStream = new FileOutputStream(assetFile);
                    final BufferedOutputStream assetBufferedOutputStream = new BufferedOutputStream(assetFileOutputStream, 8192);
                    // Write contents
                    copyWithClose(assetInputStream, assetBufferedOutputStream);
                } catch (final Exception e) {
                    // Provide a more detailed exception than the outer block
                    throw new IllegalArgumentException("Failed to write asset " + path + " to " + assetFile, e);
                }
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Unexpected error encountered in export of " + node, e);
        }
    }

    private File initializeOutputDirectory(File baseDirectory, String directoryName) {
        // Create output directory
        final File outputDirectory = new File(baseDirectory, directoryName);
        if (!outputDirectory.mkdir() && !outputDirectory.exists()) {
            throw new IllegalArgumentException("Unable to create archive output directory - " + outputDirectory);
        }
        if (outputDirectory.isFile()) {
            throw new IllegalArgumentException("Unable to export exploded directory to " + outputDirectory.getAbsolutePath() + ", it points to a existing file");
        }

        return outputDirectory;
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }

    private static void copyWithClose(InputStream input, OutputStream output) throws IOException {
        try {
            copy(input, output);
        } finally {
            try {
                input.close();
            } catch (final IOException ignore) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Could not close stream due to: " + ignore.getMessage() + "; ignoring");
                }
            }
            try {
                output.close();
            } catch (final IOException ignore) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Could not close stream due to: " + ignore.getMessage() + "; ignoring");
                }
            }
        }
    }
}
