package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.ApproveUserCommand;
import com.mottu.ops.application.dto.ApproveUserResultDto;
import com.mottu.ops.application.dto.PendingUserDto;
import com.mottu.ops.application.dto.RejectUserCommand;
import com.mottu.ops.application.dto.RejectUserResultDto;

import java.util.List;

public interface ManagerUserUseCase {
    List<PendingUserDto> getPendingUsers();
    ApproveUserResultDto approveUser(ApproveUserCommand command);
    RejectUserResultDto rejectUser(RejectUserCommand command);
}
