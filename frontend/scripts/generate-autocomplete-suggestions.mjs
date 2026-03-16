import fs from "node:fs";

function extractJsonPathsFromSchema(schema, base = "$") {
    const paths = [];
    if (schema.type === "object" && schema.properties) {
        for (const key in schema.properties) {
            const newBase = `${base}.${key}`;
            paths.push(newBase);
            paths.push(...extractJsonPathsFromSchema(schema.properties[key], newBase));
        }
    } else if (schema.type === "array" && schema.items) {
        const arrayBase = `${base}[]`;
        paths.push(arrayBase);
        paths.push(...extractJsonPathsFromSchema(schema.items, arrayBase));
    }
    return paths;
}

fs.readdirSync("./scripts")
    .filter((it) => it.endsWith(".schema.json"))
    .forEach((schemaFile) => {
        const schema = JSON.parse(fs.readFileSync(`./scripts/${schemaFile}`, "utf-8"));
        const allPaths = extractJsonPathsFromSchema(schema);
        const outputPath = `./src/components/JsonPathAutocomplete/${schemaFile.replace(".schema.json", ".json")}`;
        fs.writeFileSync(outputPath, JSON.stringify(allPaths, null, 2));
        console.log(`Preprocessed JSON paths of ${schemaFile}, saved as ${outputPath}`);
    });
