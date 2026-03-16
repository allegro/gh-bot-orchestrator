import { Avatar, Burger, Divider, Group, Menu, Title, UnstyledButton } from "@mantine/core";
import { IconBrandGithub, IconLogout, IconSettings, IconUser } from "@tabler/icons-react";
import { Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";

export function AppHeader({
    handlers,
}: {
    handlers: {
        mobileOpened: boolean;
        desktopOpened: boolean;
        toggleMobile: () => void;
        toggleDesktop: () => void;
    };
}) {
    const { i18n } = useTranslation();

    return (
        <Group h="100%" px="md" justify="space-between">
            <Group>
                <Burger color="white" opened={handlers.mobileOpened} onClick={handlers.toggleMobile} hiddenFrom="sm" size="sm" />
                <Burger color="white" opened={handlers.desktopOpened} onClick={handlers.toggleDesktop} visibleFrom="sm" size="sm" />
                <UnstyledButton component={Link} href="/">
                    <Group>
                        <IconBrandGithub color="white" size={30} />
                        <Title c="white" order={3} style={{ whiteSpace: "nowrap" }}>
                            GitHub Workflow Orchestrator
                        </Title>
                    </Group>
                </UnstyledButton>
            </Group>
            <Menu position="bottom-end" withArrow arrowPosition="center">
                <Menu.Target>
                    <Avatar src="https://thispersondoesnotexist.com" radius="xl" size="md" style={{ cursor: "pointer" }} />
                </Menu.Target>
                <Menu.Dropdown>
                    <Menu.Label>John Doe</Menu.Label>
                    <Menu.Item leftSection={<IconUser size={14} />}>Profile</Menu.Item>
                    <Menu.Item leftSection={<IconSettings size={14} />}>Settings</Menu.Item>
                    <Menu.Item>
                        <button type="button" onClick={() => changeLanguage("en")}>
                            en
                        </button>
                        <button type="button" onClick={() => changeLanguage("pl")}>
                            pl
                        </button>
                    </Menu.Item>

                    <Divider />
                    <Menu.Item leftSection={<IconLogout size={14} />} color="red">
                        Logout
                    </Menu.Item>
                </Menu.Dropdown>
            </Menu>
        </Group>
    );

    function changeLanguage(lng: string) {
        i18n.changeLanguage(lng);
    }
}
