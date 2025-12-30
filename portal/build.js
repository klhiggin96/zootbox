/**
 * Production Build Script for ZootBox Portal
 *
 * This script:
 * 1. Minifies all JavaScript files using Terser
 * 2. Removes console.* calls (already handled by logger utility)
 * 3. Copies HTML files to dist/
 * 4. Creates production-ready build
 *
 * Usage: node build.js
 */

const fs = require('fs').promises;
const path = require('path');
const { minify } = require('terser');
const { glob } = require('glob');

// Build configuration
const CONFIG = {
  sourceDir: __dirname,
  outputDir: path.join(__dirname, 'dist'),
  jsGlob: 'js/**/*.js',
  htmlFiles: ['index.html', 'machine-settings.html', 'jam-management.html', 'product-links.html', 'debug.html'],
  terserOptions: {
    compress: {
      drop_console: true,  // Remove all console.* calls
      drop_debugger: true, // Remove debugger statements
      pure_funcs: ['console.log', 'console.info', 'console.debug', 'console.warn'], // Extra safety
      passes: 2            // Run compression twice for better results
    },
    mangle: {
      toplevel: true      // Mangle top-level variable names
    },
    format: {
      comments: false     // Remove all comments
    }
  }
};

/**
 * Main build function
 */
async function build() {
  console.log('🚀 Starting ZootBox Portal production build...\n');

  try {
    // Clean dist directory
    await cleanDist();

    // Create dist directory structure
    await createDistStructure();

    // Minify JavaScript files
    await minifyJavaScript();

    // Copy HTML files
    await copyHTMLFiles();

    // Copy assets (if any)
    await copyAssets();

    // Create build metadata
    await createBuildMetadata();

    console.log('\n✅ Build completed successfully!');
    console.log(`📦 Output directory: ${CONFIG.outputDir}`);
    console.log('\n💡 To serve production build:');
    console.log('   npm run serve:prod');

  } catch (error) {
    console.error('\n❌ Build failed:', error);
    process.exit(1);
  }
}

/**
 * Clean dist directory
 */
async function cleanDist() {
  console.log('🧹 Cleaning dist directory...');

  try {
    await fs.rm(CONFIG.outputDir, { recursive: true, force: true });
    console.log('   ✓ Dist directory cleaned');
  } catch (error) {
    // Ignore if directory doesn't exist
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }
}

/**
 * Create dist directory structure
 */
async function createDistStructure() {
  console.log('📁 Creating directory structure...');

  const dirs = [
    CONFIG.outputDir,
    path.join(CONFIG.outputDir, 'js'),
    path.join(CONFIG.outputDir, 'js', 'api'),
    path.join(CONFIG.outputDir, 'js', 'components'),
    path.join(CONFIG.outputDir, 'js', 'state'),
    path.join(CONFIG.outputDir, 'js', 'utils'),
    path.join(CONFIG.outputDir, 'assets'),
  ];

  for (const dir of dirs) {
    await fs.mkdir(dir, { recursive: true });
  }

  console.log('   ✓ Directory structure created');
}

/**
 * Minify JavaScript files
 */
async function minifyJavaScript() {
  console.log('⚙️  Minifying JavaScript files...');

  // Find all JavaScript files
  const jsFiles = await glob(CONFIG.jsGlob, { cwd: CONFIG.sourceDir });

  let processedCount = 0;
  let totalSizeBefore = 0;
  let totalSizeAfter = 0;

  for (const file of jsFiles) {
    const inputPath = path.join(CONFIG.sourceDir, file);
    const outputPath = path.join(CONFIG.outputDir, file);

    // Read source file
    const code = await fs.readFile(inputPath, 'utf-8');
    totalSizeBefore += code.length;

    // Minify
    try {
      const result = await minify(code, CONFIG.terserOptions);

      if (!result.code) {
        throw new Error('Minification produced empty output');
      }

      // Ensure output directory exists
      await fs.mkdir(path.dirname(outputPath), { recursive: true });

      // Write minified file
      await fs.writeFile(outputPath, result.code, 'utf-8');

      totalSizeAfter += result.code.length;
      processedCount++;

      const reduction = ((1 - result.code.length / code.length) * 100).toFixed(1);
      console.log(`   ✓ ${file} (-${reduction}%)`);

    } catch (error) {
      console.error(`   ✗ Failed to minify ${file}:`, error.message);
      throw error;
    }
  }

  const overallReduction = ((1 - totalSizeAfter / totalSizeBefore) * 100).toFixed(1);
  console.log(`\n   📊 Summary: ${processedCount} files minified (-${overallReduction}% total size)`);
}

/**
 * Copy HTML files
 */
async function copyHTMLFiles() {
  console.log('📄 Copying HTML files...');

  for (const file of CONFIG.htmlFiles) {
    const inputPath = path.join(CONFIG.sourceDir, file);
    const outputPath = path.join(CONFIG.outputDir, file);

    try {
      await fs.copyFile(inputPath, outputPath);
      console.log(`   ✓ ${file}`);
    } catch (error) {
      if (error.code === 'ENOENT') {
        console.log(`   ⚠ ${file} not found, skipping`);
      } else {
        throw error;
      }
    }
  }
}

/**
 * Copy assets (images, icons, etc.)
 */
async function copyAssets() {
  console.log('🖼️  Copying assets...');

  const assetsSource = path.join(CONFIG.sourceDir, 'assets');
  const assetsOutput = path.join(CONFIG.outputDir, 'assets');

  try {
    // Check if assets directory exists
    await fs.access(assetsSource);

    // Copy recursively
    await copyDirectory(assetsSource, assetsOutput);

    console.log('   ✓ Assets copied');
  } catch (error) {
    if (error.code === 'ENOENT') {
      console.log('   ⚠ No assets directory found, skipping');
    } else {
      throw error;
    }
  }
}

/**
 * Create build metadata file
 */
async function createBuildMetadata() {
  console.log('📝 Creating build metadata...');

  const metadata = {
    version: require('./package.json').version,
    buildTime: new Date().toISOString(),
    environment: 'production',
    nodeVersion: process.version
  };

  // Try to get git hash
  try {
    const { execSync } = require('child_process');
    metadata.gitHash = execSync('git rev-parse --short HEAD').toString().trim();
    metadata.gitBranch = execSync('git rev-parse --abbrev-ref HEAD').toString().trim();
  } catch (error) {
    // Git not available or not a git repository
    metadata.gitHash = 'unknown';
    metadata.gitBranch = 'unknown';
  }

  const metadataPath = path.join(CONFIG.outputDir, 'build-metadata.json');
  await fs.writeFile(metadataPath, JSON.stringify(metadata, null, 2), 'utf-8');

  console.log('   ✓ Build metadata created');
  console.log(`   Version: ${metadata.version}`);
  console.log(`   Git: ${metadata.gitBranch}@${metadata.gitHash}`);
}

/**
 * Copy directory recursively
 */
async function copyDirectory(source, destination) {
  await fs.mkdir(destination, { recursive: true });

  const entries = await fs.readdir(source, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(source, entry.name);
    const destPath = path.join(destination, entry.name);

    if (entry.isDirectory()) {
      await copyDirectory(srcPath, destPath);
    } else {
      await fs.copyFile(srcPath, destPath);
    }
  }
}

// Run build
build();
