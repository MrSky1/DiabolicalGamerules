package com.diabolical.defaultgamerules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
public final class DefaultGameruleWorldData extends SavedData {
    private static final String DATA_NAME = "diabolical_default_gamerules";
    private static final Factory<DefaultGameruleWorldData> FACTORY = new Factory<>(DefaultGameruleWorldData::new, DefaultGameruleWorldData::load, DataFixTypes.LEVEL);
    private boolean synced;
    private boolean promptMessageShown;
    public DefaultGameruleWorldData() { this(false, false); }
    private DefaultGameruleWorldData(boolean synced, boolean promptMessageShown) { this.synced = synced; this.promptMessageShown = promptMessageShown; }
    public static DefaultGameruleWorldData get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME); }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean value) { if (synced != value) { synced = value; setDirty(); } }
    public boolean isPromptMessageShown() { return promptMessageShown; }
    public void setPromptMessageShown(boolean value) { if (promptMessageShown != value) { promptMessageShown = value; setDirty(); } }
    public static DefaultGameruleWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new DefaultGameruleWorldData(tag.getBoolean("default_gamerules_synced"), tag.getBoolean("default_gamerules_prompt_message"));
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("default_gamerules_synced", synced);
        tag.putBoolean("default_gamerules_prompt_message", promptMessageShown);
        return tag;
    }
}