import { Group, Title, UnstyledButton } from "@mantine/core";
import { IconBrandGithub } from "@tabler/icons-react";
import { Link } from "@tanstack/react-router";

export function AppHeader() {
    return (
        <Group h="100%" px="md">
            <UnstyledButton component={Link} href="/">
                <Group>
                    <IconBrandGithub color="white" size={30} />
                    <Title c="white" order={3} style={{ whiteSpace: "nowrap" }}>
                        GitHub Workflow Orchestrator
                    </Title>
                </Group>
            </UnstyledButton>
        </Group>
    );
}
