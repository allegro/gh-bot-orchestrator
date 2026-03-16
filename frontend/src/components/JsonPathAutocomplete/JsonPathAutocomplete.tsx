import { useState } from "react";
import { Autocomplete, AutocompleteProps } from "@mantine/core";
import pullRequestSuggestions from "./pull-request.json";

export default function JsonPathAutocomplete(props: AutocompleteProps) {
    const [value, setValue] = useState("");
    const [suggestions, setSuggestions] = useState(pullRequestSuggestions);

    function handleChange(val: string) {
        setValue(val);
        const filtered = pullRequestSuggestions.filter((path) => path.includes(val));
        setSuggestions(filtered);
    }

    return <Autocomplete placeholder="$.repository.name" data={suggestions} value={value} onChange={handleChange} limit={10} {...props} />;
}
