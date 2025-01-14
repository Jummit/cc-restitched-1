/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.network.FriendlyByteBuf;

public class ComputerContainerData implements ContainerData
{
    private final int id;
    private final ComputerFamily family;

    public ComputerContainerData( ServerComputer computer )
    {
        id = computer.getInstanceID();
        family = computer.getFamily();
    }

    public ComputerContainerData( FriendlyByteBuf byteBuf )
    {
        id = byteBuf.readInt();
        family = byteBuf.readEnum( ComputerFamily.class );
    }

    @Override
    public void toBytes( FriendlyByteBuf buf )
    {
        buf.writeInt( id );
        buf.writeEnum( family );
    }

    public int getInstanceId()
    {
        return id;
    }

    public ComputerFamily getFamily()
    {
        return family;
    }
}
