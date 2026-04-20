/*
 * This file is part of InteractiveChatDiscordSrvAddon2.
 *
 * Copyright (C) 2020 - 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2020 - 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.kelpwing.kelpylandiaplugin.chat.objectholders;

import com.kelpwing.kelpylandiaplugin.chat.objectholders.ValuePairs;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DiscordMessageContent {

    private String authorName;
    private String authorIconUrl;
    private String title;
    private List<String> description;
    private List<String> imageUrl;
    private String thumbnail;
    private List<Field> fields;
    private int color;
    private String footer;
    private String footerImageUrl;
    private Map<String, byte[]> attachments;

    public DiscordMessageContent(String authorName, String authorIconUrl, List<String> description, List<String> imageUrl, int color, Map<String, byte[]> attachments) {
        this.authorName = authorName;
        this.authorIconUrl = authorIconUrl;
        this.description = description;
        this.imageUrl = imageUrl;
        this.fields = new ArrayList<>();
        this.color = color;
        this.attachments = attachments;
        this.footer = null;
        this.footerImageUrl = null;
    }

    public DiscordMessageContent(String authorName, String authorIconUrl, String description, String imageUrl, Color color) {
        this(authorName, authorIconUrl, new ArrayList<>(Arrays.asList(description)), new ArrayList<>(Arrays.asList(imageUrl)), color.getRGB(), new HashMap<>());
    }

    public DiscordMessageContent(String authorName, String authorIconUrl, String description, String imageUrl, int color) {
        this(authorName, authorIconUrl, new ArrayList<>(Arrays.asList(description)), new ArrayList<>(Arrays.asList(imageUrl)), color, new HashMap<>());
    }

    public DiscordMessageContent(String authorName, String authorIconUrl, Color color) {
        this(authorName, authorIconUrl, new ArrayList<>(), new ArrayList<>(), color.getRGB(), new HashMap<>());
    }

    public DiscordMessageContent(String authorName, String authorIconUrl, int color) {
        this(authorName, authorIconUrl, new ArrayList<>(), new ArrayList<>(), color, new HashMap<>());
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorIconUrl() {
        return authorIconUrl;
    }

    public void setAuthorIconUrl(String authorIconUrl) {
        this.authorIconUrl = authorIconUrl;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getDescriptions() {
        return description;
    }

    public void setDescriptions(List<String> description) {
        this.description = description;
    }

    public void addDescription(String description) {
        this.description.add(description);
    }

    public void setDescription(int index, String description) {
        this.description.set(index, description);
    }

    public void clearDescriptions() {
        description.clear();
    }

    public List<String> getImageUrls() {
        return imageUrl;
    }

    public void setImageUrls(List<String> imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void addImageUrl(String imageUrl) {
        this.imageUrl.add(imageUrl);
    }

    public void setImageUrl(int index, String imageUrl) {
        this.imageUrl.set(index, imageUrl);
    }

    public void clearImageUrls() {
        imageUrl.clear();
    }

    public List<Field> getFields() {
        return fields;
    }

    public void addFields(Field... field) {
        fields.addAll(Arrays.asList(field));
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getFooterImageUrl() {
        return footerImageUrl;
    }

    public void setFooterImageUrl(String footerImageUrl) {
        this.footerImageUrl = footerImageUrl;
    }

    public Map<String, byte[]> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, byte[]> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(String name, byte[] data) {
        attachments.put(name, data);
    }

    public void clearAttachments() {
        attachments.clear();
    }

    public net.dv8tion.jda.api.requests.RestAction<java.util.List<net.dv8tion.jda.api.entities.Message>> toJDAMessageRestAction(net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel) {
        ValuePairs<List<MessageEmbed>, Set<String>> pair = toJDAMessageEmbeds();
        List<MessageEmbed> embeds = pair.getFirst();
        Set<String> neededAttachmentNames = pair.getSecond();
        List<net.dv8tion.jda.api.utils.FileUpload> files = new ArrayList<>();
        for (Entry<String, byte[]> att : attachments.entrySet()) {
            if (neededAttachmentNames.contains(att.getKey())) {
                files.add(net.dv8tion.jda.api.utils.FileUpload.fromData(att.getValue(), att.getKey()));
            }
        }
        net.dv8tion.jda.api.requests.restaction.MessageCreateAction action = channel.sendMessageEmbeds(embeds);
        for (net.dv8tion.jda.api.utils.FileUpload fu : files) {
            action = action.addFiles(fu);
        }
        return action.map(msg -> java.util.Collections.singletonList(msg));
    }

    public ValuePairs<List<MessageEmbed>, Set<String>> toJDAMessageEmbeds() {
        List<Set<String>> actions = new ArrayList<>();
        List<MessageEmbed> list = new ArrayList<>();
        Set<String> embeddedAttachments = new HashSet<>();
        EmbedBuilder embed = new EmbedBuilder().setAuthor(authorName, null, authorIconUrl).setColor(color).setThumbnail(thumbnail).setTitle(title);
        if (thumbnail != null && thumbnail.startsWith("attachment://")) {
            embeddedAttachments.add(thumbnail.substring(13));
        }
        for (Field field : fields) {
            embed.addField(field);
        }
        if (description.size() > 0) {
            embed.setDescription(description.get(0));
        }
        if (imageUrl.size() > 0) {
            String url = imageUrl.get(0);
            embed.setImage(url);
            if (url.startsWith("attachment://")) {
                embeddedAttachments.add(url.substring(13));
            }
        }
        if (imageUrl.size() == 1 || description.size() == 1) {
            if (footer != null) {
                if (footerImageUrl == null) {
                    embed.setFooter(footer);
                } else {
                    embed.setFooter(footer, footerImageUrl);
                    if (footerImageUrl.startsWith("attachment://")) {
                        embeddedAttachments.add(footerImageUrl.substring(13));
                    }
                }
            }
        }
        list.add(embed.build());
        for (int i = 1; i < imageUrl.size() || i < description.size(); i++) {
            Set<String> usedAttachments = new HashSet<>();
            EmbedBuilder otherEmbed = new EmbedBuilder().setColor(color);
            if (i < imageUrl.size()) {
                String url = imageUrl.get(i);
                otherEmbed.setImage(url);
                usedAttachments.add(url);
                if (url.startsWith("attachment://")) {
                    embeddedAttachments.add(url.substring(13));
                }
            }
            if (i < description.size()) {
                otherEmbed.setDescription(description.get(i));
            }
            if (!(i + 1 < imageUrl.size() || i + 1 < description.size())) {
                if (footer != null) {
                    if (footerImageUrl == null) {
                        otherEmbed.setFooter(footer);
                    } else {
                        otherEmbed.setFooter(footer, footerImageUrl);
                        if (footerImageUrl.startsWith("attachment://")) {
                            embeddedAttachments.add(footerImageUrl.substring(13));
                        }
                    }
                }
            }
            if (!otherEmbed.isEmpty()) {
                list.add(otherEmbed.build());
                actions.add(usedAttachments);
            }
        }
        for (Set<String> neededUrls : actions) {
            for (Entry<String, byte[]> attachment : attachments.entrySet()) {
                String attachmentName = attachment.getKey();
                if (neededUrls.contains("attachment://" + attachmentName)) {
                    embeddedAttachments.add(attachmentName);
                }
            }
        }
        return new ValuePairs<>(list, embeddedAttachments);
    }

}
