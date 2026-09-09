package peli1gamer.ourclient.settings;

import net.minecraft.core.BlockPos;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BlockPosSetting extends Setting<BlockPos> {
    private BlockPosSetting(Builder builder) { super(builder.name, builder.title, builder.description, builder.defaultValue, builder.visible, builder.onChanged); }
    @Override protected BlockPos sanitize(BlockPos value) { return value == null ? BlockPos.ZERO : value.immutable(); }
    @Override public String displayValue() { return get().getX() + ", " + get().getY() + ", " + get().getZ(); }
    @Override public boolean parse(String text) {
        if (text == null) return false;
        String[] p = text.trim().split("[, ]+");
        if (p.length != 3) return false;
        try { set(new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]))); return true; }
        catch (RuntimeException ignored) { return false; }
    }
    public static final class Builder {
        private String name, title, description = "";
        private BlockPos defaultValue = BlockPos.ZERO;
        private Supplier<Boolean> visible;
        private Consumer<BlockPos> onChanged;
        public Builder name(String v) { name = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder description(String v) { description = v; return this; }
        public Builder defaultValue(BlockPos v) { defaultValue = v; return this; }
        public Builder visible(Supplier<Boolean> v) { visible = v; return this; }
        public Builder onChanged(Consumer<BlockPos> v) { onChanged = v; return this; }
        public BlockPosSetting build() { return new BlockPosSetting(this); }
    }
}
